package com.cf.analysis.bll;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.cf.analysis.crawler.CodeforcesApiCaller;
import com.cf.analysis.crawler.CodeforcesSourceCodeCrawler;
import com.cf.analysis.crawler.CodeforcesSourceCodeCrawler.SubmissionSourceCode;
import com.cf.analysis.dal.SubmissionDAO;
import com.cf.analysis.dal.UserDAO;
import com.cf.analysis.model.user.User;

/**
 * BLL - Nghiệp vụ Crawl submission.
 *
 * Hỗ trợ:
 * - crawlAll()       : Crawl tất cả users ngay lập tức (background thread)
 * - crawlSingleUser(): Crawl một user cụ thể
 * - startSchedule()  : Bật lịch crawl tự động mỗi X giờ
 * - stopSchedule()   : Tắt lịch
 *
 * Tất cả public methods CÓ LOG CALLBACK để cập nhật UI real-time.
 */
public class CrawlService {

    private final UserDAO userDAO;
    private final SubmissionDAO submissionDAO;
    private final CodeforcesApiCaller cfClient;
    private final CodeforcesSourceCodeCrawler crawler;
    private final SettingsService settings;

    // Scheduler cho crawl định kỳ
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledTask;

    // Flag: đang crawl hay không
    private volatile boolean crawling = false;

    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");

    public CrawlService(UserDAO userDAO, SubmissionDAO submissionDAO, CodeforcesApiCaller cfClient, SettingsService settings) {
        this.userDAO = userDAO;
        this.submissionDAO = submissionDAO;
        this.cfClient = cfClient;
        this.crawler = new CodeforcesSourceCodeCrawler(cfClient);
        this.settings = settings;
    }

    public void crawlAll(Consumer<String> logCallback, Consumer<Integer> doneCallback) {
        if (crawling) {
            log(logCallback, "Cảnh báo: Đang có crawl session đang chạy, vui lòng đợi...");
            return;
        }

        new Thread(() -> {
            crawling = true;
            int totalNew = 0;

            try {
                List<User> users = userDAO.findAll();
                if (users.isEmpty()) {
                    log(logCallback, "Thông tin: Chưa có nick nào trong hệ thống! Vào tab Quản Lý Nick để thêm.");
                    return;
                }

                log(logCallback, "Bắt đầu crawl " + users.size() + " nick...");

                // Kiểm tra và tạo login session nếu chưa có
                if (!isLoginSessionAvailable()) {
                    log(logCallback, "Chưa có session đăng nhập!");
                    log(logCallback, "Đang mở browser để đăng nhập...");
                    log(logCallback, "Vui lòng đăng nhập thủ công trong browser.");

                    crawler.saveLoginSession();
                    log(logCallback, "Đã lưu session đăng nhập thành công!");
                }

                log(logCallback, "Khởi tạo browser...");
                crawler.initBrowser();

                for (User user : users) {
                    if (!crawling) break;
                    totalNew += crawlSingleUser(user.getHandle(), logCallback);
                }

                log(logCallback, "Hoàn tất! Tổng cộng: +" + totalNew + " submissions mới.");

            } catch (Exception e) {
                log(logCallback, "Lỗi crawl: " + e.getMessage());
                e.printStackTrace();
            } finally {
                crawler.closeBrowser();
                log(logCallback, "Đã đóng browser");
                crawling = false;
                int finalTotal = totalNew;
                if (doneCallback != null) doneCallback.accept(finalTotal);
            }
        }, "crawl-all-thread").start();
    }

    public int crawlSingleUser(String handle, Consumer<String> logCallback) {
        int newCount = 0;

        try {
            log(logCallback, "Crawling: " + handle + " ...");

            long maxExistingId = submissionDAO.getMaxSubmissionId(handle);
            int maxCount = settings.getMaxSubmissionsPerCrawl();

            List<SubmissionSourceCode> results = crawler.crawlUserSubmissions(handle, maxCount, maxExistingId);

            log(logCallback, "  -> Crawled " + results.size() + " submissions (sequential)");

            for (SubmissionSourceCode result : results) {
                if (!crawling) break;

                result.submission.setSourceCode(result.sourceCode);
                submissionDAO.insert(result.submission);
                newCount++;
            }

            userDAO.updateLastCrawl(handle, LocalDateTime.now());
            log(logCallback, "  " + handle + ": +" + newCount + " submissions");

        } catch (Exception e) {
            log(logCallback, "  Lỗi crawl " + handle + ": " + e.getMessage());
            e.printStackTrace();
        }

        return newCount;
    }

    // ==================== Schedule ====================

    /**
     * Bật lịch crawl tự động.
     *
     * @param intervalHours Số giờ giữa các lần crawl
     * @param logCallback   Callback log để hiển thị trên UI
     */
    public void startSchedule(int intervalHours, Consumer<String> logCallback) {
        stopSchedule(); // Dừng lịch cũ nếu có

        // Dùng daemon thread để không giữ JVM sống khi app đóng
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "crawl-scheduler");
            t.setDaemon(true);
            return t;
        });

        scheduledTask = scheduler.scheduleAtFixedRate(
            () -> crawlAll(logCallback, null),
            intervalHours, // Delay trước lần chạy đầu tiên
            intervalHours, // Interval
            TimeUnit.HOURS
        );

        log(logCallback, "Lịch crawl tự động mỗi " + intervalHours + " giờ đã được bật.");
    }

    /** Tắt lịch crawl định kỳ. */
    public void stopSchedule() {
        if (scheduledTask != null) { scheduledTask.cancel(false); scheduledTask = null; }
        if (scheduler != null) { scheduler.shutdown();        scheduler     = null; }
    }

    /** Yêu cầu dừng crawl đang chạy (graceful stop). */
    public void stopCrawl() { crawling = false; }

    public boolean isCrawling() { return crawling; }

    // ==================== Util ====================

    private void log(Consumer<String> cb, String msg) {
        if (cb == null) return;
        String time = timeFormat.format(LocalDateTime.now());
        cb.accept("[" + time + "] " + msg);
    }

    private boolean isLoginSessionAvailable() {
        return Files.exists(Paths.get("state.json"));
    }
}
