package com.cf.analysis.ui.panels;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;

import com.cf.analysis.bll.SettingsService;
import com.cf.analysis.db.DatabaseConnection;
import com.cf.analysis.ui.MainFrame;
import com.cf.analysis.ui.controllers.SettingsController;

import net.miginfocom.swing.MigLayout;

/**
 * Panel Tab 5: ⚙️ Cài Đặt.
 *
 * Nơi người dùng cấu hình:
 * 1. PostgreSQL connection (host, port, database, user, password)
 * 2. Google Gemini API Key
 * 3. Lịch crawl (số giờ giữa các lần crawl, max submissions)
 *
 * Tất cả cài đặt được lưu bền vững:
 * - DB config → file "db-config.properties"
 * - Gemini key, crawl settings → bảng "settings" trong PostgreSQL
 */
public class SettingsPanel extends JPanel {

    private final SettingsService    settingsService = new SettingsService();
    private final DatabaseConnection dbConn          = new DatabaseConnection();
    private final SettingsController controller;

    // ====== DB Config Fields ======
    private JTextField    hostField;
    private JTextField    portField;
    private JTextField    dbNameField;
    private JTextField    dbUserField;
    private JPasswordField dbPassField;
    private JLabel        dbStatusLabel;

    // ====== Gemini API ======
    private JPasswordField apiKeyField;
    private JLabel         apiStatusLabel;

    // ====== Crawl Settings ======
    private JSpinner crawlIntervalSpinner;
    private JSpinner maxSubsSpinner;

    private final MainFrame mainFrame;

    // ==================== Constructor ====================

    public SettingsPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.controller = new SettingsController(settingsService);

        // Dùng MigLayout với wrap 1 (mỗi section xuống dòng)
        setLayout(new MigLayout("insets 20, wrap 1", "[grow, fill]", ""));

        buildUI();
        loadCurrentSettings();
    }

    // ==================== Build UI ====================

    private void buildUI() {
        add(buildDbSection());
        add(buildGeminiSection(), "gaptop 16");
        add(buildCrawlSection(), "gaptop 16");
    }

    /**
     * Section 1: PostgreSQL Config.
     */
    private JPanel buildDbSection() {
        JPanel section = createSection("Kết Nối PostgreSQL");

        hostField   = new JTextField("localhost");
        portField   = new JTextField("5432");
        dbNameField = new JTextField("codeforces_analysis");
        dbUserField = new JTextField("postgres");
        dbPassField = new JPasswordField();

        // Dòng host + port
        section.add(new JLabel("Host:"));
        section.add(hostField, "w 200!, split 3");
        section.add(new JLabel("  Port:"));
        section.add(portField, "w 70!, wrap");

        // Dòng database + user
        section.add(new JLabel("Database:"));
        section.add(dbNameField, "w 200!, split 3");
        section.add(new JLabel("  User:"));
        section.add(dbUserField, "w 120!, wrap");

        // Dòng password
        section.add(new JLabel("Password:"));
        section.add(dbPassField, "w 200!, wrap");

        // Buttons + status
        JButton testBtn = new JButton("Test Kết Nối");
        JButton saveBtn = new JButton("Lưu & Kết Nối");
        dbStatusLabel   = new JLabel("");
        dbStatusLabel.setFont(new Font("Arial", Font.PLAIN, 12));

        section.add(new JLabel(""));
        section.add(testBtn, "split 3");
        section.add(saveBtn);
        section.add(dbStatusLabel, "wrap");

        // Hint
        JLabel hint = new JLabel("Cần tạo database trước: CREATE DATABASE codeforces_analysis;");
        hint.setFont(new Font("Arial", Font.ITALIC, 11));
        hint.setForeground(Color.GRAY);
        section.add(new JLabel(""));
        section.add(hint, "wrap");

        // ====== Events ======
        testBtn.addActionListener(e -> {
            applyDbConfig();
            // boolean ok = dbConn.testConnection();
            // setStatus(dbStatusLabel, ok ? "✅ Kết nối thành công!" : "❌ Kết nối thất bại!", ok);
        });

        saveBtn.addActionListener(e -> {
            applyDbConfig();
            try {
                // dbConn.connect();
                // dbConn.runMigrations(); // Flyway apply V1→V4
                // setStatus(dbStatusLabel, "✅ Đã kết nối và migrate schema thành công! (V" + dbConn.getMigrationCount() + " migrations)", true);
            } catch (Exception ex) {
                setStatus(dbStatusLabel, "❌ " + ex.getMessage(), false);
            }
        });

        return section;
    }

    /**
     * Section 2: Gemini API Key.
     */
    private JPanel buildGeminiSection() {
        JPanel section = createSection("Google Gemini API Key");

        apiKeyField = new JPasswordField();
        apiKeyField.setFont(new Font("Arial", Font.PLAIN, 13));

        section.add(new JLabel("API Key:"));
        section.add(apiKeyField, "growx, wrap");

        JLabel hintLabel = new JLabel("Lấy API Key miễn phí: https://aistudio.google.com/app/apikey  (free tier 15 req/min)");
        hintLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        hintLabel.setForeground(new Color(150, 150, 150));
        section.add(new JLabel(""));
        section.add(hintLabel, "wrap");

        JButton showHideBtn = new JButton("Hiện/Ẩn");
        JButton saveKeyBtn  = new JButton("Lưu API Key");
        apiStatusLabel = new JLabel("");
        apiStatusLabel.setFont(new Font("Arial", Font.PLAIN, 12));

        section.add(new JLabel(""));
        section.add(showHideBtn, "split 3");
        section.add(saveKeyBtn);
        section.add(apiStatusLabel, "wrap");

        // Events
        showHideBtn.addActionListener(e -> {
            char echo = apiKeyField.getEchoChar();
            apiKeyField.setEchoChar(echo == 0 ? '•' : (char)0);
        });

        saveKeyBtn.addActionListener(e -> {
            String key = new String(apiKeyField.getPassword()).trim();
            if (key.isEmpty()) {
                setStatus(apiStatusLabel, "Vui lòng nhập API Key!", false);
                return;
            }
            controller.setGeminiApiKey(key)
                .thenRun(() -> {
                    setStatus(apiStatusLabel, "Đã lưu API Key!", true);
                })
                .exceptionally(ex -> {
                    setStatus(apiStatusLabel, ex.getMessage(), false);
                    return null;
                });
        });

        return section;
    }

    /**
     * Section 3: Crawl Settings.
     */
    private JPanel buildCrawlSection() {
        JPanel section = createSection("Cài Đặt Crawl Định Kỳ");

        crawlIntervalSpinner = new JSpinner(new SpinnerNumberModel(24, 1, 168, 1));
        maxSubsSpinner       = new JSpinner(new SpinnerNumberModel(50, 10, 500, 10));

        section.add(new JLabel("Crawl mỗi:"));
        section.add(crawlIntervalSpinner, "w 70!, split 2");
        section.add(new JLabel(" giờ  (1–168)"), "wrap");

        section.add(new JLabel("Max submissions/lần:"));
        section.add(maxSubsSpinner, "w 70!, split 2");
        section.add(new JLabel(" submissions (10–500)"), "wrap");

        JButton saveCrawlBtn = new JButton("Lưu Cài Đặt Crawl");
        section.add(new JLabel(""));
        section.add(saveCrawlBtn, "wrap");

        saveCrawlBtn.addActionListener(e -> {
            int intervalHours = (Integer) crawlIntervalSpinner.getValue();
            int maxSubs = (Integer) maxSubsSpinner.getValue();

            controller.setCrawlIntervalHours(intervalHours)
                .thenCompose(v -> controller.setMaxSubmissionsPerCrawl(maxSubs))
                .thenRun(() -> {
                    JOptionPane.showMessageDialog(mainFrame,
                        "Đã lưu cài đặt crawl!",
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                })
                .exceptionally(ex -> {
                    JOptionPane.showMessageDialog(mainFrame,
                        "Lỗi: " + ex.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return null;
                });
        });

        return section;
    }

    // ==================== Data ====================

    /**
     * Đọc cài đặt hiện tại và điền vào form.
     */
    private void loadCurrentSettings() {
        // DB config (từ file properties)
        // hostField.setText(dbConn.getHost());
        // portField.setText(dbConn.getPort());
        // dbNameField.setText(dbConn.getDatabase());
        // dbUserField.setText(dbConn.getUsername());
        // dbPassField.setText(dbConn.getPassword());

        // Gemini key + crawl settings (từ DB - có thể fail nếu chưa kết nối)
        try {
            String key = controller.getGeminiApiKey();
            apiKeyField.setText(key);
            crawlIntervalSpinner.setValue(controller.getCrawlIntervalHours());
            maxSubsSpinner.setValue(controller.getMaxSubmissionsPerCrawl());
        } catch (Exception e) {
            // DB chưa kết nối → bỏ qua, dùng giá trị mặc định
        }
    }

    /**
     * Áp dụng config từ form vào DatabaseConnection (không lưu file ngay).
     */
    private void applyDbConfig() {
        // dbConn.updateConfig(
        //     hostField.getText().trim(),
        //     portField.getText().trim(),
        //     dbNameField.getText().trim(),
        //     dbUserField.getText().trim(),
        //     new String(dbPassField.getPassword())
        // );
    }

    // ==================== Util ====================

    /**
     * Tạo một section panel có border với tiêu đề.
     * Dùng MigLayout với 2 cột: label (right-align) và field (grow).
     */
    private JPanel createSection(String title) {
        JPanel panel = new JPanel(new MigLayout("insets 12 15 12 15", "[right][grow, fill]", "[]5[]"));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(70, 70, 85)),
            title,
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 13),
            new Color(150, 180, 220)
        ));
        return panel;
    }

    private void setStatus(JLabel label, String msg, boolean success) {
        label.setText(msg);
        label.setForeground(success ? new Color(180, 180, 180) : new Color(200, 200, 200));
    }
}
