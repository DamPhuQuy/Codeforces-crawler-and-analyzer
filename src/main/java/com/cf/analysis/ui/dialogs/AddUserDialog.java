package com.cf.analysis.ui.dialogs;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import com.cf.analysis.bll.UserService;
import com.cf.analysis.model.user.User;

import net.miginfocom.swing.MigLayout;

public class AddUserDialog extends JDialog {

    private final UserService userService;

    private JTextField handleField;
    private JLabel statusLabel;
    private JButton okButton;
    private JButton cancelButton;

    private boolean success = false;

    public AddUserDialog(JFrame parent, UserService userService) {
        super(parent, "Thêm Nick Codeforces", true);
        this.userService = userService;

        setLayout(new MigLayout("insets 20 25 20 25", "[right][grow, fill]", "[]10[]20[]"));
        setResizable(false);

        buildUI();
        pack();
        setMinimumSize(new Dimension(420, 0));
        setLocationRelativeTo(parent);
    }

    private void buildUI() {
        JLabel hint = new JLabel("<html><b>Nhập Codeforces Handle</b><br>" +
                "<small style='color:gray'>Ví dụ: tourist, Petr, jiangly, Um_nik</small></html>");
        add(hint, "span 2, wrap, gapbottom 5");

        add(new JLabel("Handle:"));
        handleField = new JTextField(22);
        handleField.setFont(new Font("Arial", Font.PLAIN, 14));
        handleField.setToolTipText("Nhập Codeforces handle chính xác (case-sensitive)");
        add(handleField, "wrap");

        statusLabel = new JLabel(" "); // Khoảng trắng để giữ chiều cao layout
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

        add(okButton,     "span 2, split 2, right");
        add(cancelButton);

        // event listeners

        handleField.addActionListener(e -> okButton.doClick());

        okButton.addActionListener(e -> performAdd());

        cancelButton.addActionListener(e -> dispose());

        // Auto focus vào text field khi dialog mở
        SwingUtilities.invokeLater(() -> handleField.requestFocusInWindow());
    }

    private void performAdd() {
        String handle = handleField.getText().trim();
        if (handle.isEmpty()) {
            showStatus("[!] Vui lòng nhập handle!", Color.YELLOW);
            return;
        }

        setInputEnabled(false);
        showStatus("Đang kiểm tra trên Codeforces...", new Color(180, 180, 180));

        SwingWorker<User, Void> worker = new SwingWorker<>() {

            @Override
            protected User doInBackground() throws Exception {
                // Đây chạy trên BACKGROUND THREAD - có thể gọi API/DB
                return userService.addUser(handle);
            }

            @Override
            protected void done() {
                try {
                    User user = get();
                    showStatus(
                        "[OK] Đã thêm: " + user.getHandle()
                        + " | Rating: " + user.getRating()
                        + " | " + user.getRank(),
                        new Color(180, 180, 180)
                    );
                    success = true;

                    Timer closeTimer = new Timer(1500, te -> dispose());
                    closeTimer.setRepeats(false);
                    closeTimer.start();

                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    showStatus("[X] " + cause.getMessage(), new Color(200, 200, 200));
                    setInputEnabled(true);
                    handleField.selectAll();
                    handleField.requestFocusInWindow();
                }
            }
        };

        worker.execute();
    }

    private void showStatus(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
    }

    private void setInputEnabled(boolean enabled) {
        handleField.setEnabled(enabled);
        okButton.setEnabled(enabled);
    }

    public boolean isSuccess() {
        return success;
    }
}
