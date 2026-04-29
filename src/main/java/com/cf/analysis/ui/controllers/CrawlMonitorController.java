package com.cf.analysis.ui.controllers;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.cf.analysis.bll.CrawlService;
import com.cf.analysis.bll.SettingsService;

/**
 * Controller cho Crawl Monitor Panel.
 * Xử lý logic điều khiển crawling submissions.
 */
public class CrawlMonitorController {

    private final CrawlService crawlService;
    private final SettingsService settingsService;

    public CrawlMonitorController(CrawlService crawlService, SettingsService settingsService) {
        this.crawlService = crawlService;
        this.settingsService = settingsService;
    }

    /**
     * Crawl submissions cho một user.
     *
     * @param handle User handle
     * @param logCallback Callback để log tiến trình
     * @return Số lượng submissions đã crawl
     */
    public int crawlSingleUser(String handle, Consumer<String> logCallback) {
        return crawlService.crawlSingleUser(handle, logCallback);
    }

    /**
     * Crawl submissions cho tất cả users (async).
     *
     * @param logCallback Callback để log tiến trình
     * @param doneCallback Callback khi hoàn tất với tổng số submissions
     */
    public void crawlAllUsersAsync(Consumer<String> logCallback, Consumer<Integer> doneCallback) {
        crawlService.crawlAll(logCallback, doneCallback);
    }

    /**
     * Bật lịch crawl tự động.
     *
     * @param intervalHours Số giờ giữa các lần crawl
     * @param logCallback Callback để log
     */
    public void startSchedule(int intervalHours, Consumer<String> logCallback) {
        crawlService.startSchedule(intervalHours, logCallback);
    }

    /**
     * Tắt lịch crawl tự động.
     */
    public void stopSchedule() {
        crawlService.stopSchedule();
    }

    /**
     * Dừng crawl đang chạy.
     */
    public void stopCrawl() {
        crawlService.stopCrawl();
    }

    /**
     * Kiểm tra xem đang crawl hay không.
     */
    public boolean isCrawling() {
        return crawlService.isCrawling();
    }

    /**
     * Lấy giới hạn crawl mặc định từ settings.
     */
    public int getMaxSubmissionsPerCrawl() {
        return settingsService.getMaxSubmissionsPerCrawl();
    }

    /**
     * Lưu giới hạn crawl mặc định.
     */
    public void setMaxSubmissionsPerCrawl(int max) {
        try {
            settingsService.setMaxSubmissionsPerCrawl(max);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi lưu cài đặt: " + e.getMessage(), e);
        }
    }

    /**
     * Lấy crawl interval (giờ).
     */
    public int getCrawlIntervalHours() {
        return settingsService.getCrawlIntervalHours();
    }

    /**
     * Lưu crawl interval (giờ).
     */
    public void setCrawlIntervalHours(int hours) {
        try {
            settingsService.setCrawlIntervalHours(hours);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi lưu cài đặt: " + e.getMessage(), e);
        }
    }

    /**
     * Validate crawl limit.
     */
    public boolean isValidCrawlLimit(int limit) {
        return limit > 0 && limit <= 1000; // Max 1000 submissions per user
    }

    /**
     * Validate interval hours.
     */
    public boolean isValidIntervalHours(int hours) {
        return hours >= 1 && hours <= 168; // 1 hour to 1 week
    }
}
