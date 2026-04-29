package com.cf.analysis.bll;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.cf.analysis.crawler.CodeforcesApiCaller;
import com.cf.analysis.dal.SubmissionDAO;
import com.cf.analysis.dal.UserDAO;
import com.cf.analysis.model.submission.Submission;
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
    private final SettingsService settings;

    // Scheduler cho crawl định kỳ
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?>       scheduledTask;

    // Flag: đang crawl hay không
    private volatile boolean crawling = false;

    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");

    public CrawlService(UserDAO userDAO, SubmissionDAO submissionDAO, CodeforcesApiCaller cfClient, SettingsService settings) {
        this.userDAO = userDAO;
        this.submissionDAO = submissionDAO;
        this.cfClient = cfClient;
        this.settings = settings;
    }

    // ==================== Crawl All ====================

    /**
     * Crawl tất cả users trong hệ thống.
     * Chạy trên background thread → KHÔNG block UI.
     *
     * @param logCallback  Nhận mỗi dòng log để hiển thị trên UI
     * @param doneCallback Nhận tổng số submission mới khi hoàn tất
     */
    public void crawlAll(Consumer<String> logCallback, Consumer<Integer> doneCallback) {
        if (crawling) {
            log(logCallback, "⚠️ Đang có crawl session đang chạy, vui lòng đợi...");
            return;
        }

        new Thread(() -> {
            crawling = true;
            int totalNew = 0;

            try {
                List<User> users = userDAO.findAll();
                if (users.isEmpty()) {
                    log(logCallback, "ℹ️ Chưa có nick nào trong hệ thống! Vào tab Quản Lý Nick để thêm.");
                    return;
                }

                log(logCallback, "🚀 Bắt đầu crawl " + users.size() + " nick...");

                for (User user : users) {
                    if (!crawling) break; // Cho phép dừng giữa chừng
                    totalNew += crawlSingleUser(user.getHandle(), logCallback);
                }

                log(logCallback, "✅ Hoàn tất! Tổng cộng: +" + totalNew + " submissions mới.");

            } catch (Exception e) {
                log(logCallback, "❌ Lỗi crawl: " + e.getMessage());
            } finally {
                crawling = false;
                int finalTotal = totalNew;
                if (doneCallback != null) doneCallback.accept(finalTotal);
            }
        }, "crawl-all-thread").start();
    }

    // ==================== Crawl Single User ====================

    /**
     * Crawl một user cụ thể.
     * Có thể gọi trực tiếp từ UI (blocking trên calling thread).
     *
     * @return Số lượng submission mới được thêm vào DB
     */
    public int crawlSingleUser(String handle, Consumer<String> logCallback) {
        int newCount = 0;

        try {
            log(logCallback, "📥 Crawling: " + handle + " ...");

            // Lấy submission_id cao nhất đã có → chỉ crawl mới hơn
            long maxExistingId = submissionDAO.getMaxSubmissionId(handle);
            int  maxCount      = settings.getMaxSubmissionsPerCrawl();

            // Gọi CF API lấy danh sách submission mới
            List<Submission> newSubs = cfClient.getUserSubmissions(handle, maxCount, maxExistingId);
            log(logCallback, "  → Tìm thấy " + newSubs.size() + " submission mới (Accepted)");

            // Với mỗi submission: lấy source code rồi lưu vào DB
            for (int i = 0; i < newSubs.size(); i++) {
                if (!crawling && i > 0) break; // Kiểm tra stop flag

                Submission sub = newSubs.get(i);
                log(logCallback, "  → [" + (i + 1) + "/" + newSubs.size() + "] "
                        + sub.getProblemName() + " - lấy source code...");

                try {
                    String code = cfClient.getSubmissionSourceCode(
                        sub.getContestId(), sub.getSubmissionId()
                    );
                    sub.setSourceCode(code);
                    if (code == null) {
                        log(logCallback, "    ⚠️ Code bị ẩn hoặc không truy cập được.");
                    }
                } catch (Exception e) {
                    log(logCallback, "    ⚠️ Không lấy được code: " + e.getMessage());
                }

                // Lưu vào DB (ON CONFLICT DO NOTHING nên an toàn)
                submissionDAO.insert(sub);
                newCount++;
            }

            // Cập nhật thời gian crawl cuối
            userDAO.updateLastCrawl(handle, LocalDateTime.now());
            log(logCallback, "  ✅ " + handle + ": +" + newCount + " submissions");

        } catch (Exception e) {
            log(logCallback, "  ❌ Lỗi crawl " + handle + ": " + e.getMessage());
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

        log(logCallback, "⏰ Lịch crawl tự động mỗi " + intervalHours + " giờ đã được bật.");
    }

    /** Tắt lịch crawl định kỳ. */
    public void stopSchedule() {
        if (scheduledTask != null) { scheduledTask.cancel(false); scheduledTask = null; }
        if (scheduler     != null) { scheduler.shutdown();        scheduler     = null; }
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
}
