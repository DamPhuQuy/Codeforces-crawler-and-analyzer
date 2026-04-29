package com.cf.analysis.ui.dialogs;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import com.cf.analysis.bll.UserService;
import com.cf.analysis.model.user.User;

import net.miginfocom.swing.MigLayout;

public class AddUserDialog extends JDialog {

    private final UserService userService;

    private JTextArea handleField;
    private JLabel statusLabel;
    private JButton okButton;
    private JButton cancelButton;

    private boolean success = false;

    private static final int MAX_HANDLES = 10;

    public AddUserDialog(JFrame parent, UserService userService) {
        super(parent, "Thêm Nick Codeforces", true);
        this.userService = userService;

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
        handleField.setToolTipText("Nhập handles, mỗi handle một dòng hoặc cách nhau bởi dấu phẩy");
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

        add(okButton,     "span 2, split 2, right");
        add(cancelButton);

        // event listeners
        okButton.addActionListener(e -> performAdd());
        cancelButton.addActionListener(e -> dispose());

        SwingUtilities.invokeLater(() -> handleField.requestFocusInWindow());
    }

    private void performAdd() {
        String input = handleField.getText().trim();
        if (input.isEmpty()) {
            showStatus("[!] Vui lòng nhập ít nhất một handle!", Color.YELLOW);
            return;
        }

        // Parse handles: tách theo dòng mới hoặc dấu phẩy
        List<String> handles = Arrays.stream(input.split("[,\\n\\r]+"))
            .map(String::trim)
            .filter(h -> !h.isEmpty())
            .distinct()
            .collect(Collectors.toList());

        if (handles.isEmpty()) {
            showStatus("[!] Không tìm thấy handle hợp lệ!", Color.YELLOW);
            return;
        }

        if (handles.size() > MAX_HANDLES) {
            showStatus("[!] Tối đa " + MAX_HANDLES + " handles! Hiện tại: " + handles.size(), Color.YELLOW);
            return;
        }

        setInputEnabled(false);
        showStatus("Đang thêm " + handles.size() + " handle(s)...", new Color(180, 180, 180));

        SwingWorker<List<String>, String> worker = new SwingWorker<>() {
            private int successCount = 0;
            private int failCount = 0;
            private List<String> errors = new ArrayList<>();

            @Override
            protected List<String> doInBackground() throws Exception {
                List<String> results = new ArrayList<>();

                for (int i = 0; i < handles.size(); i++) {
                    String handle = handles.get(i);
                    publish("Đang xử lý " + (i + 1) + "/" + handles.size() + ": " + handle);

                    try {
                        // Delay giữa các request để tránh rate limit (200ms)
                        if (i > 0) {
                            Thread.sleep(200);
                        }

                        User user = userService.addUser(handle);
                        successCount++;
                        results.add("[OK] " + user.getHandle() + " (Rating: " + user.getRating() + ")");
                    } catch (Exception ex) {
                        failCount++;
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        String errMsg = "[X] " + handle + ": " + cause.getMessage();
                        errors.add(errMsg);
                        results.add(errMsg);
                    }
                }

                return results;
            }

            @Override
            protected void process(List<String> chunks) {
                // Cập nhật status trong khi đang xử lý
                if (!chunks.isEmpty()) {
                    showStatus(chunks.get(chunks.size() - 1), new Color(180, 180, 180));
                }
            }

            @Override
            protected void done() {
                try {
                    get(); // Đợi hoàn thành

                    String summary = String.format("Hoàn tất: %d thành công, %d thất bại",
                        successCount, failCount);
                    showStatus(summary, successCount > 0 ? new Color(180, 180, 180) : new Color(200, 200, 200));

                    if (successCount > 0) {
                        success = true;
                        // Tự đóng sau 2 giây
                        Timer closeTimer = new Timer(2000, te -> dispose());
                        closeTimer.setRepeats(false);
                        closeTimer.start();
                    } else {
                        setInputEnabled(true);
                        handleField.selectAll();
                        handleField.requestFocusInWindow();
                    }

                } catch (Exception ex) {
                    showStatus("[X] Lỗi: " + ex.getMessage(), new Color(200, 200, 200));
                    setInputEnabled(true);
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
