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
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;

import com.cf.analysis.ui.MainFrame;
import com.cf.analysis.ui.controllers.SettingsController;

import net.miginfocom.swing.MigLayout;

public class SettingsPanel extends JPanel {

    private final String FONT_NAME = "Arial";

    private final SettingsController controller;

    private JPasswordField apiKeyField;
    private JLabel apiStatusLabel;

    private JSpinner crawlIntervalSpinner;
    private JSpinner maxSubsSpinner;

    private final MainFrame mainFrame;

    public SettingsPanel(MainFrame mainFrame, SettingsController controller) {
        this.mainFrame = mainFrame;
        this.controller = controller;

        setLayout(new MigLayout("insets 20, wrap 1", "[grow, fill]", ""));

        buildUI();
        loadCurrentSettings();
    }

    private void buildUI() {
        add(buildGeminiSection(), "gaptop 16");
        add(buildCrawlSection(), "gaptop 16");
    }

    private JPanel buildGeminiSection() {
        JPanel section = createSection("Google Gemini API Key");

        apiKeyField = new JPasswordField();
        apiKeyField.setFont(new Font(FONT_NAME, Font.PLAIN, 13));

        section.add(new JLabel("API Key:"));
        section.add(apiKeyField, "growx, wrap");

        JLabel hintLabel = new JLabel("Lấy API Key tại: https://aistudio.google.com/apikey");
        hintLabel.setFont(new Font(FONT_NAME, Font.ITALIC, 11));
        hintLabel.setForeground(new Color(150, 150, 150));
        section.add(new JLabel(""));
        section.add(hintLabel, "wrap");

        JButton showHideBtn = new JButton("Hiện/Ẩn");
        JButton saveKeyBtn = new JButton("Lưu API Key");
        apiStatusLabel = new JLabel("");
        apiStatusLabel.setFont(new Font(FONT_NAME, Font.PLAIN, 12));

        section.add(new JLabel(""));
        section.add(showHideBtn, "split 3");
        section.add(saveKeyBtn);
        section.add(apiStatusLabel, "wrap");

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

    private JPanel buildCrawlSection() {
        JPanel section = createSection("Cài Đặt Crawl Định Kỳ");

        crawlIntervalSpinner = new JSpinner(new SpinnerNumberModel(24, 1, 168, 1));
        maxSubsSpinner = new JSpinner(new SpinnerNumberModel(50, 10, 500, 10));

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

    private void loadCurrentSettings() {
        try {
            String key = controller.getGeminiApiKey();
            apiKeyField.setText(key);
            crawlIntervalSpinner.setValue(controller.getCrawlIntervalHours());
            maxSubsSpinner.setValue(controller.getMaxSubmissionsPerCrawl());
        } catch (Exception e) {
            // Ignore load errors, just show empty/default values
        }
    }

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
