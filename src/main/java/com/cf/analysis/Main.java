package com.cf.analysis;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.cf.analysis.ui.MainFrame;
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

            // Cấu hình font hỗ trợ tiếng Việt
            Font defaultFont = new Font("Arial", Font.PLAIN, 13);
            Font boldFont = new Font("Arial", Font.BOLD, 13);

            UIManager.put("Label.font", defaultFont);
            UIManager.put("Button.font", defaultFont);
            UIManager.put("TextField.font", defaultFont);
            UIManager.put("TextArea.font", defaultFont);
            UIManager.put("ComboBox.font", defaultFont);
            UIManager.put("Table.font", defaultFont);
            UIManager.put("TableHeader.font", boldFont);
            UIManager.put("TabbedPane.font", defaultFont);
            UIManager.put("Menu.font", defaultFont);
            UIManager.put("MenuItem.font", defaultFont);
            UIManager.put("CheckBox.font", defaultFont);
            UIManager.put("RadioButton.font", defaultFont);
            UIManager.put("TitledBorder.font", boldFont);

        } catch (Exception e) {
            System.err.println("Không thể cài FlatDarkLaf, dùng theme mặc định: " + e.getMessage());
        }

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
