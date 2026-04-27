package com.cf.analysis;

import java.awt.Color;

import javax.swing.UIManager;

import com.cf.analysis.db.Database;
import com.cf.analysis.db.DatabaseConnection;
import com.formdev.flatlaf.FlatDarkLaf;
public class Main {

    public static void main(String[] args) {
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

        try {
            Database db = new DatabaseConnection();
            db.getConnection();
        } catch (Exception e) {
            System.err.println("Chưa kết nối được DB: " + e.getMessage());
        }

        // SwingUtilities.invokeLater(() -> {
        //     try {
        //         MainFrame frame = new MainFrame();
        //         frame.setVisible(true);
        //     } catch (Exception e) {
        //         JOptionPane.showMessageDialog(null,
        //             "Lỗi khởi động ứng dụng:\n" + e.getMessage(),
        //             "Lỗi", JOptionPane.ERROR_MESSAGE);
        //         e.printStackTrace();
        //     }
        // });
    }
}
