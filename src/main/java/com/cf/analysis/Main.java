package com.cf.analysis;

import com.cf.analysis.db.DatabaseConnection;
import com.cf.analysis.ui.MainFrame;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import java.awt.*;

/**
 * Entry point của ứng dụng Codeforces Examination Analysis.
 *
 * Thứ tự khởi động:
 * 1. Cài đặt Look & Feel hiện đại (FlatDarkLaf)
 * 2. Cố gắng kết nối PostgreSQL + khởi tạo schema
 * 3. Mở MainFrame trên Event Dispatch Thread (EDT)
 */
public class Main {

    public static void main(String[] args) {
        // ====== Bước 1: Cài Look & Feel ======
        // Phải làm TRƯỚC khi tạo bất kỳ Swing component nào
        try {
            FlatDarkLaf.setup();

            // Tuỳ chỉnh appearance
            UIManager.put("Component.arc", 8);
            UIManager.put("Button.arc", 8);
            UIManager.put("TextComponent.arc", 6);
            UIManager.put("ScrollBar.showButtons", false);
            UIManager.put("TabbedPane.tabHeight", 42);
            UIManager.put("Table.rowHeight", 28);
            UIManager.put("Table.alternateRowColor", new Color(35, 35, 40));
            UIManager.put("TitlePane.background", new Color(30, 30, 35));

        } catch (Exception e) {
            System.err.println("Không thể cài FlatDarkLaf, dùng theme mặc định: " + e.getMessage());
        }

        // ====== Bước 2: Cố gắng kết nối DB + chạy Flyway migrations ======
        // Chạy TRƯỚC khi mở GUI để schema sẵn sàng ngay khi app bắt đầu.
        // Nếu thất bại (chưa cài PostgreSQL, sai password...) thì vẫn mở app
        // để user có thể vào tab Cài Đặt để cấu hình lại.
        try {
            DatabaseConnection db = DatabaseConnection.getInstance();
            db.connect();
            db.runMigrations(); // Flyway tự động apply V1→V4
        } catch (Exception e) {
            System.err.println("⚠️ Chưa kết nối được DB (sẽ cấu hình trong app): " + e.getMessage());
        }

        // ====== Bước 3: Chạy GUI trên EDT ======
        // QUAN TRỌNG: Tất cả Swing code phải chạy trên EDT (Event Dispatch Thread)
        // SwingUtilities.invokeLater() đảm bảo điều này
        SwingUtilities.invokeLater(() -> {
            try {
                MainFrame frame = new MainFrame();
                frame.setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                    "Lỗi khởi động ứng dụng:\n" + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        });
    }
}
