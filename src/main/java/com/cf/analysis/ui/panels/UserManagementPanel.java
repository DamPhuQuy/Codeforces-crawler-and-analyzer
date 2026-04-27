package com.cf.analysis.ui.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.cf.analysis.bll.UserService;
import com.cf.analysis.model.user.User;
import com.cf.analysis.ui.MainFrame;
import com.cf.analysis.ui.dialogs.AddUserDialog;

import net.miginfocom.swing.MigLayout;

/**
 * Panel Tab 1: 👥 Quản Lý Nick Codeforces.
 *
 * Chức năng:
 * - Hiển thị bảng danh sách tất cả nick đã thêm
 * - Thêm nick mới (qua AddUserDialog)
 * - Xóa nick đã chọn (có confirm dialog)
 * - Cập nhật rating mới nhất từ Codeforces
 *
 * Cấu trúc class (cho người mới học Swing):
 * - Constructor: gọi initComponents() và loadData()
 * - initComponents(): tạo toolbar và bảng
 * - loadData(): lấy data từ DB và đưa vào bảng (dùng SwingWorker)
 * - refreshData(): public, để MainFrame gọi khi cần reload
 */
public class UserManagementPanel extends JPanel {

    // ====== BLL (Business Logic Layer) ======
    // Panel chỉ gọi Service, không gọi DAO trực tiếp
    private final UserService userService;

    // ====== UI Components ======
    private JTable            userTable;
    private DefaultTableModel tableModel;
    private JButton           addButton;
    private JButton           deleteButton;
    private JButton           refreshRatingBtn;
    private JLabel            statusLabel;

    // Tên cột của bảng
    private static final String[] COLUMNS = {
        "Handle", "Tên hiển thị", "Rating", "Max Rating", "Rank", "Quốc gia", "Thêm lúc", "Crawl gần nhất"
    };

    private final MainFrame         mainFrame;
    private final SimpleDateFormat  sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    // ==================== Constructor ====================

    public UserManagementPanel(MainFrame mainFrame, UserService userService) {
        this.mainFrame = mainFrame;
        this.userService = userService;

        // Layout toàn bộ panel
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));

        // Tạo UI components
        initComponents();

        // Tải dữ liệu lần đầu
        loadData();
    }

    // ==================== Build UI ====================

    /**
     * Tạo tất cả UI components và thêm vào panel.
     */
    private void initComponents() {
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildTable(),   BorderLayout.CENTER);

        statusLabel = new JLabel("Sẵn sàng");
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        statusLabel.setForeground(Color.GRAY);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        add(statusLabel, BorderLayout.SOUTH);
    }

    /**
     * Tạo toolbar với 3 nút: Thêm, Xóa, Refresh Rating.
     */
    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new MigLayout("insets 0", "[][]15[]push", ""));

        // ---- Nút Thêm Nick ----
        addButton = new JButton("➕ Thêm Nick");
        addButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        addButton.setBackground(new Color(63, 81, 181));   // Indigo
        addButton.setForeground(Color.WHITE);
        addButton.setFocusPainted(false);
        addButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // ---- Nút Xóa Nick ----
        deleteButton = new JButton("🗑 Xóa Nick");
        deleteButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        deleteButton.setBackground(new Color(183, 28, 28)); // Đỏ đậm
        deleteButton.setForeground(Color.WHITE);
        deleteButton.setFocusPainted(false);
        deleteButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        deleteButton.setEnabled(false); // Tắt cho đến khi chọn row

        // ---- Nút Refresh Rating ----
        refreshRatingBtn = new JButton("🔃 Cập Nhật Rating");
        refreshRatingBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        refreshRatingBtn.setFocusPainted(false);
        refreshRatingBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshRatingBtn.setEnabled(false);

        toolbar.add(addButton);
        toolbar.add(deleteButton);
        toolbar.add(refreshRatingBtn);

        // ====== Gắn sự kiện (Event Listeners) ======

        // Nút Thêm: mở AddUserDialog, sau đó reload nếu thành công
        addButton.addActionListener(e -> {
            AddUserDialog dialog = new AddUserDialog(mainFrame, userService);
            dialog.setVisible(true); // Blocking (modal dialog)
            if (dialog.isSuccess()) {
                loadData();
                setStatus("✅ Đã thêm nick mới thành công!");
            }
        });

        // Nút Xóa: confirm rồi xóa
        deleteButton.addActionListener(e -> handleDelete());

        // Nút Refresh Rating: cập nhật rating từ CF API
        refreshRatingBtn.addActionListener(e -> handleRefreshRating());

        return toolbar;
    }

    /**
     * Tạo JTable để hiển thị danh sách users.
     *
     * Ghi chú cho người mới:
     * - DefaultTableModel: class mặc định của Swing để quản lý data trong JTable
     * - Mỗi row = một Object[]
     * - isCellEditable=false: không cho user chỉnh sửa trực tiếp trên bảng
     */
    private JScrollPane buildTable() {
        // Tạo model với tên cột
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Không cho sửa trực tiếp
            }
        };

        userTable = new JTable(tableModel);

        // Cài đặt cơ bản
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        userTable.getTableHeader().setReorderingAllowed(false);
        userTable.setRowHeight(30);
        userTable.setShowGrid(false);
        userTable.setIntercellSpacing(new Dimension(0, 2));

        // Căn giữa cột số
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        userTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer); // Rating
        userTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer); // Max Rating

        // Màu sắc cho cột Rank (theo màu CF)
        userTable.getColumnModel().getColumn(4).setCellRenderer(new RankColorRenderer());

        // Gắn sự kiện chọn row
        userTable.getSelectionModel().addListSelectionListener(e -> {
            boolean hasSelection = userTable.getSelectedRow() != -1;
            deleteButton.setEnabled(hasSelection);
            refreshRatingBtn.setEnabled(hasSelection);
        });

        // Double click → Refresh rating của nick đó
        userTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) handleRefreshRating();
            }
        });

        JScrollPane scroll = new JScrollPane(userTable);
        scroll.setBorder(BorderFactory.createTitledBorder("📋 Danh Sách Accounts Được Theo Dõi"));
        return scroll;
    }

    // ==================== Actions ====================

    /**
     * Xử lý xóa nick đang chọn.
     */
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
            try {
                userService.removeUser(handle);
                loadData();
                setStatus("✅ Đã xóa nick: " + handle);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(mainFrame,
                    "Lỗi xóa: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Cập nhật rating mới nhất từ Codeforces cho nick đang chọn.
     * Dùng SwingWorker vì gọi API mất thời gian.
     */
    private void handleRefreshRating() {
        int row = userTable.getSelectedRow();
        if (row == -1) return;

        String handle = (String) tableModel.getValueAt(row, 0);
        refreshRatingBtn.setEnabled(false);
        setStatus("🔄 Đang cập nhật rating của " + handle + "...");

        // SwingWorker: doInBackground chạy trên thread riêng, done() chạy trên EDT
        SwingWorker<User, Void> worker = new SwingWorker<>() {
            @Override
            protected User doInBackground() throws Exception {
                return userService.refreshUserInfo(handle); // Gọi CF API
            }

            @Override
            protected void done() {
                try {
                    User updated = get();
                    loadData(); // Reload bảng
                    refreshRatingBtn.setEnabled(true);
                    setStatus("✅ Cập nhật thành công: " + handle
                            + " | Rating: " + (updated != null ? updated.getRating() : "?"));
                } catch (Exception ex) {
                    refreshRatingBtn.setEnabled(true);
                    setStatus("❌ Lỗi: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    // ==================== Data Loading ====================

    /**
     * Tải dữ liệu từ DB vào bảng.
     * Dùng SwingWorker để không block UI khi đọc DB.
     */
    public void loadData() {
        SwingWorker<List<User>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<User> doInBackground() throws Exception {
                return userService.getAllUsers();
            }

            @Override
            protected void done() {
                try {
                    List<User> users = get();

                    // Xóa hết data cũ trong bảng
                    tableModel.setRowCount(0);

                    // Thêm từng user vào bảng
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

                } catch (Exception ex) {
                    setStatus("❌ Lỗi tải dữ liệu: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    /** Public: để MainFrame gọi khi cần reload. */
    public void refreshData() {
        loadData();
    }

    private void setStatus(String msg) {
        statusLabel.setText(msg);
    }

    // ==================== Custom Cell Renderers ====================

    /**
     * Custom renderer cho cột Rank: tô màu theo màu rating của Codeforces.
     * Người mới học Swing: Renderer là class xác định "dòng này trông như thế nào".
     */
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
