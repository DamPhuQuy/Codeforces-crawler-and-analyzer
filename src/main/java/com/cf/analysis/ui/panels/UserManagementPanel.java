package com.cf.analysis.ui.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.text.SimpleDateFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.cf.analysis.bll.UserService;
import com.cf.analysis.model.user.User;
import com.cf.analysis.ui.MainFrame;
import com.cf.analysis.ui.controllers.UserManagementController;
import com.cf.analysis.ui.dialogs.AddUserDialog;

import net.miginfocom.swing.MigLayout;

public class UserManagementPanel extends JPanel {

    private final UserService userService;
    private final UserManagementController controller;

    private JTable userTable;
    private DefaultTableModel tableModel;
    private JButton addButton;
    private JButton deleteButton;
    private JButton refreshRatingBtn;
    private JLabel statusLabel;

    private static final String[] COLUMNS = {
        "Handle", "Tên hiển thị", "Rating", "Max Rating", "Rank", "Quốc gia", "Thêm lúc", "Crawl gần nhất"
    };

    private final MainFrame mainFrame;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public UserManagementPanel(MainFrame mainFrame, UserService userService) {
        this.mainFrame = mainFrame;
        this.userService = userService;
        this.controller = new UserManagementController(userService);

        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));

        initComponents();
        loadData();
    }


    private void initComponents() {
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildTable(),   BorderLayout.CENTER);

        statusLabel = new JLabel("Sẵn sàng");
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        statusLabel.setForeground(Color.GRAY);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        add(statusLabel, BorderLayout.SOUTH);
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new MigLayout("insets 0", "[][]15[]push", ""));

        addButton = new JButton("Thêm Nick");
        addButton.setFont(new Font("Arial", Font.BOLD, 13));
        addButton.setBackground(new Color(80, 80, 80));
        addButton.setForeground(Color.WHITE);
        addButton.setFocusPainted(false);
        addButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        deleteButton = new JButton("Xóa Nick");
        deleteButton.setFont(new Font("Arial", Font.PLAIN, 13));
        deleteButton.setBackground(new Color(120, 120, 120));
        deleteButton.setForeground(Color.WHITE);
        deleteButton.setFocusPainted(false);
        deleteButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        deleteButton.setEnabled(false); // true when row is selected

        refreshRatingBtn = new JButton("Cập Nhật Rating");
        refreshRatingBtn.setFont(new Font("Arial", Font.PLAIN, 13));
        refreshRatingBtn.setFocusPainted(false);
        refreshRatingBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshRatingBtn.setEnabled(false);

        toolbar.add(addButton);
        toolbar.add(deleteButton);
        toolbar.add(refreshRatingBtn);


        addButton.addActionListener(e -> {
            AddUserDialog dialog = new AddUserDialog(mainFrame, userService);
            dialog.setVisible(true); // Blocking (modal dialog)
            if (dialog.isSuccess()) {
                loadData();
                setStatus("Đã thêm nick mới thành công!");
            }
        });

        deleteButton.addActionListener(e -> handleDelete());

        refreshRatingBtn.addActionListener(e -> handleRefreshRating());

        return toolbar;
    }

    private JScrollPane buildTable() {
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        userTable = new JTable(tableModel);

        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        userTable.getTableHeader().setReorderingAllowed(false);
        userTable.setRowHeight(30);
        userTable.setShowGrid(false);
        userTable.setIntercellSpacing(new Dimension(0, 2));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        userTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer); // Rating
        userTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer); // Max Rating

        userTable.getColumnModel().getColumn(4).setCellRenderer(new RankColorRenderer());

        userTable.getSelectionModel().addListSelectionListener(e -> {
            boolean hasSelection = userTable.getSelectedRow() != -1;
            deleteButton.setEnabled(hasSelection);
            refreshRatingBtn.setEnabled(hasSelection);
        });

        userTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) handleRefreshRating();
            }
        });

        JScrollPane scroll = new JScrollPane(userTable);
        scroll.setBorder(BorderFactory.createTitledBorder("Danh Sách Accounts Được Theo Dõi"));
        return scroll;
    }

    private void handleDelete() {
        int row = userTable.getSelectedRow();
        if (row == -1) return;

        String handle = (String) tableModel.getValueAt(row, 0);

        // Hỏi xác nhận trước khi xóa
        int confirm = JOptionPane.showConfirmDialog(
            mainFrame,
            "<html>Bạn có chắc muốn xóa nick <b>" + handle + "</b>?<br>" +
            "<small style='color:red'>Tất cả submissions và kết quả phân tích AI sẽ bị xóa!</small></html>",
            "Xác Nhận Xóa",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            controller.removeUserAsync(handle)
                .thenRun(() -> {
                    loadData();
                    setStatus("Đã xóa nick: " + handle);
                })
                .exceptionally(ex -> {
                    JOptionPane.showMessageDialog(mainFrame,
                        "Lỗi xóa: " + ex.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return null;
                });
        }
    }

    private void handleRefreshRating() {
        int row = userTable.getSelectedRow();
        if (row == -1) return;

        String handle = (String) tableModel.getValueAt(row, 0);
        refreshRatingBtn.setEnabled(false);
        setStatus("Đang cập nhật rating của " + handle + "...");

        controller.refreshUserInfoAsync(handle)
            .thenAccept(updated -> {
                loadData(); // Reload bảng
                refreshRatingBtn.setEnabled(true);
                setStatus("Cập nhật thành công: " + handle
                        + " | Rating: " + (updated != null ? updated.getRating() : "?"));
            })
            .exceptionally(ex -> {
                refreshRatingBtn.setEnabled(true);
                setStatus("Lỗi: " + ex.getMessage());
                return null;
            });
    }

    public void loadData() {
        controller.getAllUsersAsync()
            .thenAccept(users -> {
                tableModel.setRowCount(0);

                for (User u : users) {
                    tableModel.addRow(new Object[]{
                        u.getHandle(),
                        u.getDisplayName() != null ? u.getDisplayName() : "",
                        u.getRating(),
                        u.getMaxRating(),
                        u.getRank() != null ? u.getRank() : "newbie",
                        u.getCountry() != null ? u.getCountry() : "",
                        u.getAddedDate()    != null ? sdf.format(u.getAddedDate())    : "N/A",
                        u.getLastCrawlAt()  != null ? sdf.format(u.getLastCrawlAt())  : "Chưa crawl"
                    });
                }

                setStatus("Đã tải " + users.size() + " nick.");
            })
            .exceptionally(ex -> {
                setStatus("Lỗi tải dữ liệu: " + ex.getMessage());
                return null;
            });
    }

    public void refreshData() {
        loadData();
    }

    private void setStatus(String msg) {
        statusLabel.setText(msg);
    }

    private static class RankColorRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(JLabel.CENTER);

            if (!isSelected && value != null) {
                String rank = value.toString().toLowerCase();
                if      (rank.contains("legendary"))        setForeground(new Color(255, 30,  30));
                else if (rank.contains("international"))    setForeground(new Color(255, 50,  50));
                else if (rank.contains("grandmaster"))      setForeground(new Color(220, 30,  30));
                else if (rank.contains("master"))           setForeground(new Color(255, 140,  0));
                else if (rank.contains("candidate"))        setForeground(new Color(255, 140,  0));
                else if (rank.contains("expert"))           setForeground(new Color(170,   0, 170));
                else if (rank.contains("specialist"))       setForeground(new Color(  3, 168, 158));
                else if (rank.contains("pupil"))            setForeground(new Color(119, 167, 119));
                else                                        setForeground(Color.GRAY); // newbie
            }

            return this;
        }
    }
}
