package com.cf.analysis.ui.dialogs;

import com.cf.analysis.bll.UserService;
import com.cf.analysis.model.User;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog nhập Codeforces handle để thêm user mới.
 *
 * Luồng:
 * 1. Mở dialog → user nhập handle → nhấn "Thêm"
 * 2. Gọi CF API để validate + lấy thông tin (background thread)
 * 3. Nếu thành công → lưu DB → đóng dialog (sau 1.5s)
 * 4. Nếu lỗi → hiển thị lỗi → cho nhập lại
 *
 * MODAL = true: Phải đóng dialog này mới dùng được MainFrame.
 */
public class AddUserDialog extends JDialog {

    private final UserService userService;

    private JTextField  handleField;
    private JLabel      statusLabel;
    private JButton     okButton;
    private JButton     cancelButton;

    // Flag để MainFrame biết kết quả
    private boolean success = false;

    /**
     * @param parent      Frame cha (MainFrame)
     * @param userService Service để thêm user
     */
    public AddUserDialog(JFrame parent, UserService userService) {
        super(parent, "Thêm Nick Codeforces", true); // true = modal
        this.userService = userService;

        setLayout(new MigLayout("insets 20 25 20 25", "[right][grow, fill]", "[]10[]20[]"));
        setResizable(false);

        buildUI();
        pack();
        setMinimumSize(new Dimension(420, 0));
        setLocationRelativeTo(parent);
    }

    private void buildUI() {
        // Label hướng dẫn
        JLabel hint = new JLabel("<html><b>Nhập Codeforces Handle</b><br>" +
                "<small style='color:gray'>Ví dụ: tourist, Petr, jiangly, Um_nik</small></html>");
        add(hint, "span 2, wrap, gapbottom 5");

        // Text field nhập handle
        add(new JLabel("Handle:"));
        handleField = new JTextField(22);
        handleField.setFont(new Font("Monospaced", Font.PLAIN, 14));
        handleField.setToolTipText("Nhập Codeforces handle chính xác (case-sensitive)");
        add(handleField, "wrap");

        // Label status (thông báo kết quả)
        statusLabel = new JLabel(" "); // Khoảng trắng để giữ chiều cao layout
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        add(statusLabel, "span 2, wrap");

        // Nút Thêm và Hủy
        okButton = new JButton("✅ Thêm");
        okButton.setBackground(new Color(63, 81, 181));
        okButton.setForeground(Color.WHITE);
        okButton.setFocusPainted(false);
        okButton.setPreferredSize(new Dimension(110, 34));

        cancelButton = new JButton("✖ Hủy");
        cancelButton.setFocusPainted(false);
        cancelButton.setPreferredSize(new Dimension(90, 34));

        add(okButton,     "span 2, split 2, right");
        add(cancelButton);

        // ====== Event Listeners ======

        // Enter trong textfield = click OK
        handleField.addActionListener(e -> okButton.doClick());

        // Nút Thêm
        okButton.addActionListener(e -> performAdd());

        // Nút Hủy
        cancelButton.addActionListener(e -> dispose());

        // Auto focus vào text field khi dialog mở
        SwingUtilities.invokeLater(() -> handleField.requestFocusInWindow());
    }

    /**
     * Thực hiện thêm user khi nhấn OK.
     * Chạy trên background thread để không đơ UI trong khi gọi API.
     *
     * TẠI SAO DÙNG SwingWorker?
     * - Gọi CF API có thể mất 1-3s
     * - Nếu chạy trực tiếp trên EDT, UI sẽ bị đơ (không click, scroll được)
     * - SwingWorker: doInBackground() chạy trên thread riêng; done() chạy lại EDT
     */
    private void performAdd() {
        String handle = handleField.getText().trim();
        if (handle.isEmpty()) {
            showStatus("⚠️ Vui lòng nhập handle!", Color.YELLOW);
            return;
        }

        // Disable UI trong khi đang xử lý
        setInputEnabled(false);
        showStatus("🔄 Đang kiểm tra trên Codeforces...", new Color(100, 180, 255));

        // ====== SwingWorker: background task ======
        SwingWorker<User, Void> worker = new SwingWorker<>() {

            @Override
            protected User doInBackground() throws Exception {
                // Đây chạy trên BACKGROUND THREAD - có thể gọi API/DB
                return userService.addUser(handle);
            }

            @Override
            protected void done() {
                // Đây chạy trở lại EDT - có thể cập nhật UI
                try {
                    User user = get(); // Lấy kết quả hoặc ném exception nếu có lỗi
                    showStatus(
                        "✅ Đã thêm: " + user.getHandle()
                        + " | Rating: " + user.getRating()
                        + " | " + user.getRank(),
                        new Color(0, 210, 100)
                    );
                    success = true;

                    // Tự đóng dialog sau 1.5 giây
                    Timer closeTimer = new Timer(1500, te -> dispose());
                    closeTimer.setRepeats(false);
                    closeTimer.start();

                } catch (Exception ex) {
                    // Lấy root cause message
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    showStatus("❌ " + cause.getMessage(), new Color(255, 80, 80));
                    setInputEnabled(true); // Cho phép nhập lại
                    handleField.selectAll();
                    handleField.requestFocusInWindow();
                }
            }
        };

        worker.execute(); // Bắt đầu chạy background
    }

    private void showStatus(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
    }

    private void setInputEnabled(boolean enabled) {
        handleField.setEnabled(enabled);
        okButton.setEnabled(enabled);
    }

    /**
     * Kiểm tra dialog đã thêm thành công chưa.
     * MainFrame gọi sau khi dialog đóng để quyết định có reload không.
     */
    public boolean isSuccess() {
        return success;
    }
}
