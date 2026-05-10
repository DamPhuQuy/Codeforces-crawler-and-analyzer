package com.cf.analysis.crawler;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.cf.analysis.model.submission.Submission;
import com.google.common.util.concurrent.RateLimiter;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

public class CodeforcesSourceCodeCrawler {
    private static final RateLimiter rateLimiter = RateLimiter.create(0.33);
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 2000;
    private static final int PAGE_TIMEOUT_MS = 30000;
    private static final int SELECTOR_TIMEOUT_MS = 20000;

    private final CodeforcesApiCaller apiCaller;
    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    public CodeforcesSourceCodeCrawler(CodeforcesApiCaller apiCaller) {
        this.apiCaller = apiCaller;
    }

    public void saveLoginSession() {
        Playwright pw = Playwright.create();
        Browser br = pw.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(false)
                .setArgs(List.of("--disable-blink-features=AutomationControlled")));
        BrowserContext ctx = br.newContext();
        Page page = ctx.newPage();

        page.navigate("https://codeforces.com/enter", new Page.NavigateOptions()
                .setTimeout(PAGE_TIMEOUT_MS));
        System.out.println("Hãy đăng nhập thủ công và vượt Captcha...");

        page.waitForURL("https://codeforces.com/", new Page.WaitForURLOptions().setTimeout(60000));

        ctx.storageState(new BrowserContext.StorageStateOptions().setPath(Paths.get("state.json")));
        System.out.println("Đã lưu phiên đăng nhập vào file state.json!");

        br.close();
        pw.close();
    }

    public void initBrowser() {
        if (playwright != null) {
            return;
        }

        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(false)
                .setArgs(List.of("--disable-blink-features=AutomationControlled")));

        // Tạo context và page một lần, reuse cho tất cả submissions
        context = browser.newContext(new Browser.NewContextOptions()
                .setStorageStatePath(Paths.get("state.json")));
        page = context.newPage();

        System.out.println("Browser initialized with persistent context");
    }

    public void closeBrowser() {
        if (page != null) {
            try {
                page.close();
            } catch (Exception e) {
                System.err.println("Error closing page: " + e.getMessage());
            }
            page = null;
        }
        if (context != null) {
            try {
                context.close();
            } catch (Exception e) {
                System.err.println("Error closing context: " + e.getMessage());
            }
            context = null;
        }
        if (browser != null) {
            try {
                browser.close();
            } catch (Exception e) {
                System.err.println("Error closing browser: " + e.getMessage());
            }
            browser = null;
        }
        if (playwright != null) {
            try {
                playwright.close();
            } catch (Exception e) {
                System.err.println("Error closing playwright: " + e.getMessage());
            }
            playwright = null;
        }

        System.out.println("Browser closed");
    }

    public List<SubmissionSourceCode> crawlUserSubmissions(String handle, int maxCount, long minSubId) {
        if (browser == null) {
            throw new IllegalStateException("Browser not initialized. Call initBrowser() first.");
        }

        try {
            List<Submission> submissions = apiCaller.getUserSubmissions(handle, maxCount, minSubId);
            System.out.println("Found " + submissions.size() + " accepted submissions for " + handle);

            List<SubmissionSourceCode> results = new ArrayList<>();

            for (int i = 0; i < submissions.size(); i++) {
                Submission sub = submissions.get(i);

                System.out.println("Processing submission " + (i + 1) + "/" + submissions.size() + " (ID: " + sub.getId() + ")");

                SubmissionSourceCode result = crawlSubmissionWithRetry(sub);
                if (result != null && result.sourceCode != null) {
                    System.err.println(result.sourceCode);
                    results.add(result);
                }
            }

            System.out.println("Successfully crawled " + results.size() + "/" + submissions.size() + " submissions");
            return results;

        } catch (IOException e) {
            System.err.println("Error fetching submissions from API: " + e.getMessage());
            return List.of();
        }
    }

    private SubmissionSourceCode crawlSubmissionWithRetry(Submission submission) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                rateLimiter.acquire();
                return crawlSingleSubmission(submission);
            } catch (Exception e) {
                System.err.println("Attempt " + attempt + "/" + MAX_RETRIES + " failed for submission "
                        + submission.getId() + ": " + e.getMessage());

                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }
        }
        return null;
    }

    private SubmissionSourceCode crawlSingleSubmission(Submission submission) {
        try {
            String url = buildSubmissionUrl(submission);
            page.navigate(url, new Page.NavigateOptions()
                    .setTimeout(PAGE_TIMEOUT_MS));

            // Kiểm tra xem có bị redirect về login page không
            String currentUrl = page.url();
            if (currentUrl.contains("/enter") || currentUrl.contains("login")) {
                throw new RuntimeException("Session hết hạn - bị redirect về login page. Cần đăng nhập lại!");
            }

            // Chờ selector với timeout
            page.waitForSelector("#program-source-text", new Page.WaitForSelectorOptions()
                    .setTimeout(SELECTOR_TIMEOUT_MS));
            String sourceCode = page.locator("#program-source-text").innerText();

            System.out.println("✓ Crawled submission " + submission.getId());
            return new SubmissionSourceCode(submission, sourceCode);

        } catch (Exception e) {
            System.err.println("✗ Failed to crawl submission " + submission.getId() + ": " + e.getMessage());

            // Log thêm thông tin debug
            try {
                System.err.println("   Current URL: " + page.url());
            } catch (Exception ignored) {}

            return null;
        }
    }

    private String buildSubmissionUrl(Submission submission) {
        if (submission.getContestId() != null) {
            return String.format("https://codeforces.com/contest/%d/submission/%d",
                    submission.getContestId(), submission.getId());
        } else {
            return String.format("https://codeforces.com/problemset/submission/%d", submission.getId());
        }
    }

    public static class SubmissionSourceCode {
        public final Submission submission;
        public final String sourceCode;

        public SubmissionSourceCode(Submission submission, String sourceCode) {
            this.submission = submission;
            this.sourceCode = sourceCode;
        }
    }
}
