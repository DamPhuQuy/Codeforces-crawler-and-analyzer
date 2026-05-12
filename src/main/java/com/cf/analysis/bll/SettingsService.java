package com.cf.analysis.bll;

import java.sql.SQLException;

import com.cf.analysis.dal.SettingsDAO;

public class SettingsService {

    // Tên các key trong bảng settings
    public static final String KEY_GEMINI_API = "gemini_api_key";
    public static final String KEY_CRAWL_INTERVAL = "crawl_interval_hours";
    public static final String KEY_MAX_SUBS = "max_submissions_per_crawl";
    public static final String KEY_ACCEPTED_ONLY = "crawl_accepted_only";

    private final SettingsDAO settingsDAO;

    public SettingsService(SettingsDAO settingsDAO) {
        this.settingsDAO = settingsDAO;
    }

    public String getGeminiApiKey() {
        try {
            return settingsDAO.get(KEY_GEMINI_API, "");
        } catch (SQLException e) {
            System.err.println("Loi doc Gemini key: " + e.getMessage());
            return "";
        }
    }

    public void setGeminiApiKey(String key) throws SQLException {
        settingsDAO.set(KEY_GEMINI_API, key);
    }

    public int getCrawlIntervalHours() {
        try {
            return Integer.parseInt(settingsDAO.get(KEY_CRAWL_INTERVAL, "24"));
        } catch (Exception e) { return 24; }
    }

    public void setCrawlIntervalHours(int hours) throws SQLException {
        settingsDAO.set(KEY_CRAWL_INTERVAL, String.valueOf(hours));
    }

    public int getMaxSubmissionsPerCrawl() {
        try {
            return Integer.parseInt(settingsDAO.get(KEY_MAX_SUBS, "20"));
        } catch (Exception e) { return 20; }
    }

    public void setMaxSubmissionsPerCrawl(int max) throws SQLException {
        settingsDAO.set(KEY_MAX_SUBS, String.valueOf(max));
    }

    public boolean isCrawlAcceptedOnly() {
        try {
            return Boolean.parseBoolean(settingsDAO.get(KEY_ACCEPTED_ONLY, "true"));
        } catch (Exception e) { return true; }
    }
}
