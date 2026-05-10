package com.cf.analysis.bll;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

    public CrawlService(UserDAO userDAO, SubmissionDAO submissionDAO, CodeforcesApiCaller cfClient, SettingsService settings, CodeforcesSourceCodeCrawler crawler) {
        this.userDAO = userDAO;
        this.submissionDAO = submissionDAO;
        this.cfClient = cfClient;
        this.crawler = crawler;
        this.settings = settings;
    }

    public void crawlAll(Consumer<String> logCallback, Consumer<Integer> doneCallback) {
        if (crawling) {
            log(logCallback, "Canh bao: Dang co crawl dang chay.");
            return;
        }

        new Thread(() -> {
            crawling = true;
            int totalNew = 0;

            try {
                List<User> users = userDAO.findAll();
                if (users.isEmpty()) {
                    log(logCallback, "Khong co tai khoan nao de crawl!");
                    return;
                }

                log(logCallback, "Bat dau crawl " + users.size() + " tai khoan...");

                // Kiểm tra session
                if (!isLoginSessionAvailable()) {
                    log(logCallback, "Chua co session dang nhap!");
                    log(logCallback, "Mo browser de dang nhap...");
                    log(logCallback, "Dang nhap thu cong trong browser.");

                    crawler.saveLoginSession();
                    log(logCallback, "Da luu session dang nhap!");
                } else if (isSessionExpired()) {
                    log(logCallback, "Session dang nhap da het han!");
                    log(logCallback, "Mo browser de dang nhap lai...");
                    log(logCallback, "Dang nhap thu cong trong browser.");

                    crawler.saveLoginSession();
                    log(logCallback, "Da luu session dang nhap moi!");
                }

                log(logCallback, "Khoi tao browser...");
                crawler.initBrowser();

                for (User user : users) {
                    if (!crawling) break;
                    totalNew += crawlSingleUser(user.getHandle(), logCallback);
                }

                log(logCallback, "Tong cong: +" + totalNew + " submissions moi.");

            } catch (Exception e) {
                log(logCallback, "Loi crawl: " + e.getMessage());
                e.printStackTrace();

                // Nếu lỗi liên quan đến session, yêu cầu đăng nhập lại
                if (e.getMessage() != null && e.getMessage().contains("Session het han")) {
                    log(logCallback, "Phat hien session het han trong qua trinh crawl!");
                    log(logCallback, "Vui long chay lai va dang nhap lai.");
                }
            } finally {
                crawler.closeBrowser();
                log(logCallback, "Da dong browser");
                crawling = false;
                int finalTotal = totalNew;
                if (doneCallback != null) doneCallback.accept(finalTotal);
            }
        }, "crawl-all-thread").start();
    }

    public int crawlSingleUser(String handle, Consumer<String> logCallback) {
        int newCount = 0;

        try {
            log(logCallback, "Dang crawl: " + handle + " ...");

            long maxExistingId = submissionDAO.getMaxSubmissionId(handle);
            int maxCount = settings.getMaxSubmissionsPerCrawl();

            log(logCallback, "-> Max submission id tu lan crawl truoc: " + maxExistingId);
            log(logCallback, "-> Max submissions duoc setting: " + maxCount);

            List<SubmissionSourceCode> results = crawler.crawlUserSubmissions(handle, maxCount, 0);

            log(logCallback, "-> Da crawl " + results.size() + " submissions");

            for (SubmissionSourceCode result : results) {
                if (!crawling) break;

                result.submission.setSourceCode(result.sourceCode);

                log(logCallback, "Submission " + result.submission.getId());
                log(logCallback, "Source code: " + (result.sourceCode.length() > 100 ? result.sourceCode.substring(0, 100) + "..." : result.sourceCode));

                submissionDAO.insert(result.submission);
                newCount++;
            }

            userDAO.updateLastCrawl(handle, LocalDateTime.now());
            log(logCallback, "  " + handle + ": +" + newCount + " submissions");

        } catch (Exception e) {
            log(logCallback, "Loi crawl " + handle + ": " + e.getMessage());
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

    /**
     * Kiểm tra xem session trong state.json có hết hạn không.
     * Đọc cookies và kiểm tra expires time.
     */
    private boolean isSessionExpired() {
        try {
            String content = Files.readString(Paths.get("state.json"));
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();

            if (!json.has("cookies")) {
                return true;
            }

            JsonArray cookies = json.getAsJsonArray("cookies");
            long currentTime = Instant.now().getEpochSecond();

            // Kiểm tra các cookies quan trọng
            for (int i = 0; i < cookies.size(); i++) {
                JsonObject cookie = cookies.get(i).getAsJsonObject();
                String name = cookie.get("name").getAsString();

                // Kiểm tra cookies quan trọng cho authentication
                if (name.equals("JSESSIONID") || name.equals("X-User-Sha1") || name.equals("cf_clearance")) {
                    if (cookie.has("expires")) {
                        double expires = cookie.get("expires").getAsDouble();

                        // expires = -1 nghĩa là session cookie (hết hạn khi đóng browser)
                        // expires > 0 là unix timestamp
                        if (expires > 0 && expires < currentTime) {
                            System.out.println("Cookie " + name + " đã hết hạn (expires: " + expires + ", now: " + currentTime + ")");
                            return true;
                        }
                    }
                }
            }

            return false;

        } catch (IOException e) {
            System.err.println("Lỗi đọc state.json: " + e.getMessage());
            return true;
        } catch (Exception e) {
            System.err.println("Lỗi parse state.json: " + e.getMessage());
            return true;
        }
    }
}
