package com.cf.analysis.crawler;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.cf.analysis.model.submission.Submission;
import com.google.common.util.concurrent.RateLimiter;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

public class CodeforcesSourceCodeCrawler {
    private static final RateLimiter rateLimiter = RateLimiter.create(0.4);
    private static final int THREAD_POOL_SIZE = 5;
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 2000;

    private final CodeforcesApiCaller apiCaller;
    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private ExecutorService executorService;

    public CodeforcesSourceCodeCrawler(CodeforcesApiCaller apiCaller) {
        this.apiCaller = apiCaller;
    }

    public void saveLoginSession() {
        try (Playwright pw = Playwright.create()) {
            Browser br = pw.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(false)
                    .setArgs(List.of("--disable-blink-features=AutomationControlled")));
            BrowserContext ctx = br.newContext();
            Page page = ctx.newPage();

            page.navigate("https://codeforces.com/enter");
            System.out.println("Hãy đăng nhập thủ công và vượt Captcha...");

            page.waitForURL("https://codeforces.com/", new Page.WaitForURLOptions().setTimeout(60000));

            ctx.storageState(new BrowserContext.StorageStateOptions().setPath(Paths.get("state.json")));
            System.out.println("Đã lưu phiên đăng nhập vào file state.json!");

            br.close();
        }
    }

    public void initBrowser() {
        if (playwright != null) {
            return;
        }

        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(false)
                .setArgs(List.of("--disable-blink-features=AutomationControlled")));
        context = browser.newContext(new Browser.NewContextOptions()
                .setStorageStatePath(Paths.get("state.json")));
        executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        System.out.println("Browser initialized with " + THREAD_POOL_SIZE + " threads");
    }

    public void closeBrowser() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (context != null) {
            context.close();
        }
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }

        playwright = null;
        browser = null;
        context = null;
        executorService = null;

        System.out.println("Browser closed");
    }

    public List<SubmissionSourceCode> crawlUserSubmissions(String handle, int maxCount, long minSubId) {
        initBrowser();

        try {
            List<Submission> submissions = apiCaller.getUserSubmissions(handle, maxCount, minSubId);
            System.out.println("Found " + submissions.size() + " accepted submissions for " + handle);

            List<CompletableFuture<SubmissionSourceCode>> futures = new ArrayList<>();

            for (Submission sub : submissions) {
                CompletableFuture<SubmissionSourceCode> future = CompletableFuture.supplyAsync(
                        () -> crawlSubmissionWithRetry(sub),
                        executorService
                );
                futures.add(future);
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            List<SubmissionSourceCode> results = new ArrayList<>();
            for (CompletableFuture<SubmissionSourceCode> future : futures) {
                try {
                    SubmissionSourceCode result = future.get();
                    if (result != null && result.sourceCode != null) {
                        results.add(result);
                    }
                } catch (Exception e) {
                    System.err.println("Error getting future result: " + e.getMessage());
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
        Page page = context.newPage();
        try {
            String url = buildSubmissionUrl(submission);
            page.navigate(url);

            page.waitForSelector("#program-source-text", new Page.WaitForSelectorOptions().setTimeout(10000));
            String sourceCode = page.locator("#program-source-text").innerText();

            System.out.println("✓ Crawled submission " + submission.getId());
            return new SubmissionSourceCode(submission, sourceCode);

        } catch (Exception e) {
            System.err.println("✗ Failed to crawl submission " + submission.getId() + ": " + e.getMessage());
            return null;
        } finally {
            page.close();
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
