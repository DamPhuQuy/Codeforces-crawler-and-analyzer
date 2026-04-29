package com.cf.analysis.ui.controllers;

import java.util.concurrent.CompletableFuture;

import com.cf.analysis.bll.SettingsService;

/**
 * Controller cho Settings Panel.
 * Xử lý logic điều khiển cài đặt hệ thống.
 */
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /**
     * Lấy Gemini API key.
     */
    public String getGeminiApiKey() {
        return settingsService.getGeminiApiKey();
    }

    /**
     * Lưu Gemini API key (async).
     */
    public CompletableFuture<Void> setGeminiApiKeyAsync(String apiKey) {
        return CompletableFuture.runAsync(() -> {
            try {
                settingsService.setGeminiApiKey(apiKey);
            } catch (Exception e) {
                throw new RuntimeException("Lỗi lưu API key: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Lấy crawl interval (giờ).
     */
    public int getCrawlIntervalHours() {
        return settingsService.getCrawlIntervalHours();
    }

    /**
     * Lưu crawl interval (async).
     */
    public CompletableFuture<Void> setCrawlIntervalHoursAsync(int hours) {
        return CompletableFuture.runAsync(() -> {
            try {
                settingsService.setCrawlIntervalHours(hours);
            } catch (Exception e) {
                throw new RuntimeException("Lỗi lưu cài đặt: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Lấy max submissions per crawl.
     */
    public int getMaxSubmissionsPerCrawl() {
        return settingsService.getMaxSubmissionsPerCrawl();
    }

    /**
     * Lưu max submissions per crawl (async).
     */
    public CompletableFuture<Void> setMaxSubmissionsPerCrawlAsync(int max) {
        return CompletableFuture.runAsync(() -> {
            try {
                settingsService.setMaxSubmissionsPerCrawl(max);
            } catch (Exception e) {
                throw new RuntimeException("Lỗi lưu cài đặt: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Lấy crawl accepted only flag.
     */
    public boolean isCrawlAcceptedOnly() {
        return settingsService.isCrawlAcceptedOnly();
    }

    /**
     * Validate API key format.
     */
    public boolean isValidApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return false;
        }
        // Gemini API key thường bắt đầu với "AIza" và dài khoảng 39 ký tự
        return apiKey.startsWith("AIza") && apiKey.length() >= 30;
    }

    /**
     * Validate crawl limit.
     */
    public boolean isValidCrawlLimit(int limit) {
        return limit > 0 && limit <= 1000;
    }

    /**
     * Validate interval hours.
     */
    public boolean isValidIntervalHours(int hours) {
        return hours >= 1 && hours <= 168; // 1 hour to 1 week
    }

    /**
     * Lấy tất cả settings keys.
     */
    public String[] getAllSettingKeys() {
        return new String[]{
            SettingsService.KEY_GEMINI_API,
            SettingsService.KEY_CRAWL_INTERVAL,
            SettingsService.KEY_MAX_SUBS,
            SettingsService.KEY_ACCEPTED_ONLY
        };
    }
}
