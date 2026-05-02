package com.cf.analysis.ui.dialogs;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.cf.analysis.ui.controllers.UserManagementController;

import net.miginfocom.swing.MigLayout;

public class AddUserDialog extends JDialog {

    private final UserManagementController controller;

    private JTextArea handleField;
    private JLabel statusLabel;
    private JButton okButton;
    private JButton cancelButton;

    private boolean success = false;

    private static final int MAX_HANDLES = 10;
    private static final Color STATUS_INFO = new Color(180, 180, 180);
    private static final Color STATUS_WARNING = Color.YELLOW;
    private static final Color STATUS_ERROR = new Color(200, 100, 100);

    public AddUserDialog(JFrame parent, UserManagementController controller) {
        super(parent, "Thêm Nick Codeforces", true);
        this.controller = controller;

        setLayout(new MigLayout("insets 20 25 20 25", "[right][grow, fill]", "[]10[]20[]"));
        setResizable(false);

        buildUI();
        pack();
        setMinimumSize(new Dimension(480, 0));
        setLocationRelativeTo(parent);
    }

    private void buildUI() {
        JLabel hint = new JLabel("<html><b>Nhập Codeforces Handle</b><br>" +
                "<small style='color:gray'>Có thể nhập nhiều handles (tối đa " + MAX_HANDLES + "), mỗi handle một dòng hoặc cách nhau bởi dấu phẩy<br>" +
                "Ví dụ: tourist, Petr, jiangly</small></html>");
        add(hint, "span 2, wrap, gapbottom 5");

        add(new JLabel("Handles:"));
        handleField = new JTextArea(4, 22);
        handleField.setFont(new Font("Arial", Font.PLAIN, 14));
        handleField.setLineWrap(true);
        handleField.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(handleField);
        scroll.setPreferredSize(new Dimension(300, 90));
        add(scroll, "wrap");

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        add(statusLabel, "span 2, wrap");

        okButton = new JButton("Thêm");
        okButton.setBackground(new Color(80, 80, 80));
        okButton.setForeground(Color.WHITE);
        okButton.setFocusPainted(false);
        okButton.setPreferredSize(new Dimension(110, 34));

        cancelButton = new JButton("Hủy");
        cancelButton.setFocusPainted(false);
        cancelButton.setPreferredSize(new Dimension(90, 34));

        add(okButton, "span 2, split 2, right");
        add(cancelButton);

        okButton.addActionListener(e -> performAdd());
        cancelButton.addActionListener(e -> dispose());

        SwingUtilities.invokeLater(() -> handleField.requestFocusInWindow());
    }

    private void performAdd() {
        List<String> handles = parseHandles();
        if (handles == null) return;

        disableInput();
        showStatus("Đang thêm " + handles.size() + " handle(s)...", STATUS_INFO);

        new Thread(() -> addHandlesInBackground(handles)).start();
    }

    private List<String> parseHandles() {
        String input = handleField.getText().trim();
        if (input.isEmpty()) {
            showStatus("Vui lòng nhập ít nhất một handle!", STATUS_WARNING);
            return null;
        }

        List<String> handles = new ArrayList<>();
        for (String part : input.split("[,\\n\\r]+")) {
            String handle = part.trim();
            if (!handle.isEmpty()) {
                handles.add(handle);
            }
        }

        if (handles.isEmpty()) {
            showStatus("Không tìm thấy handle hợp lệ!", STATUS_WARNING);
            return null;
        }

        if (handles.size() > MAX_HANDLES) {
            showStatus("Tối đa " + MAX_HANDLES + " handles! Hiện tại: " + handles.size(), STATUS_WARNING);
            return null;
        }

        return handles;
    }

    private void addHandlesInBackground(List<String> handles) {
        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < handles.size(); i++) {
            String handle = handles.get(i);
            int current = i + 1;
            int total = handles.size();

            SwingUtilities.invokeLater(() ->
                showStatus("Đang xử lý " + current + "/" + total + ": " + handle, STATUS_INFO)
            );

            try {
                if (i > 0) {
                    Thread.sleep(200);
                }

                controller.addUser(handle).get();
                successCount++;

            } catch (Exception ex) {
                failCount++;
                String errorMsg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                System.err.println("Lỗi thêm " + handle + ": " + errorMsg);
            }
        }

        int finalSuccess = successCount;
        int finalFail = failCount;

        SwingUtilities.invokeLater(() -> {
            String summary = String.format("Hoàn tất: %d thành công, %d thất bại", finalSuccess, finalFail);
            showStatus(summary, finalSuccess > 0 ? STATUS_INFO : STATUS_ERROR);

            if (finalSuccess > 0) {
                success = true;
                new Thread(() -> {
                    try {
                        Thread.sleep(1500);
                        SwingUtilities.invokeLater(() -> dispose());
                    } catch (InterruptedException ignored) {}
                }).start();
            } else {
                enableInput();
            }
        });
    }

    private void showStatus(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
    }

    private void disableInput() {
        handleField.setEnabled(false);
        okButton.setEnabled(false);
    }

    private void enableInput() {
        handleField.setEnabled(true);
        okButton.setEnabled(true);
        handleField.selectAll();
        handleField.requestFocusInWindow();
    }

    public boolean isSuccess() {
        return success;
    }
}
