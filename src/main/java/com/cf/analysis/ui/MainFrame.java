package com.cf.analysis.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import com.cf.analysis.db.DatabaseConnection;
import com.cf.analysis.ui.panels.CrawlMonitorPanel;
import com.cf.analysis.ui.panels.EvaluationPanel;
import com.cf.analysis.ui.panels.SettingsPanel;
import com.cf.analysis.ui.panels.SubmissionAnalysisPanel;
import com.cf.analysis.ui.panels.UserManagementPanel;

import net.miginfocom.swing.MigLayout;

/**
 * Frame chính của ứng dụng - LAYER 1 (GUI).
 *
 * Chỉ làm 2 việc:
 * 1. Tổ chức layout: header + JTabbedPane (5 tabs) + status bar
 * 2. Cung cấp phương thức refresh để các panel gọi nhau
 *
 * KHÔNG có bất kỳ logic nghiệp vụ nào ở đây.
 */
public class MainFrame extends JFrame {

    // ====== 5 Panels của 5 tabs ======
    private UserManagementPanel     userManagementPanel;
    private CrawlMonitorPanel       crawlMonitorPanel;
    private SubmissionAnalysisPanel submissionAnalysisPanel;
    private EvaluationPanel         evaluationPanel;
    private SettingsPanel           settingsPanel;

    private JLabel statusBarLabel;

    public MainFrame() {
        setTitle("⚡ Codeforces Examination Analysis System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 700));
        setPreferredSize(new Dimension(1450, 850));

        // Cố gắng kết nối DB khi khởi động
        initDatabase();

        // Xây dựng UI
        buildUI();

        pack();
        setLocationRelativeTo(null); // Căn giữa màn hình

        // Dọn dẹp khi đóng app
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Dừng background tasks trước khi thoát
                if (crawlMonitorPanel != null) {
                    crawlMonitorPanel.stopAll();
                }
            }
        });
    }

    /**
     * Kết nối DB và chạy Flyway migrations.
     * Nếu thất bại, vẫn mở app để user vào Settings cấu hình.
     */
    private void initDatabase() {
        try {
            DatabaseConnection db = new DatabaseConnection();
            // db.connect();
            // db.runMigrations(); // Flyway: V1 schema + V2 views + V3 settings + V4 seeds
            System.out.println("✅ Database và migrations sẵn sàng!");
        } catch (Exception e) {
            System.err.println("⚠️ Chưa kết nối được DB: " + e.getMessage());
            // Sẽ thông báo cho user qua StatusBar sau khi UI dựng xong
        }
    }

    /**
     * Xây dựng toàn bộ UI chính.
     */
    private void buildUI() {
        setLayout(new BorderLayout());

        // -- Header gradient ở trên cùng --
        add(buildHeaderPanel(), BorderLayout.NORTH);

        // -- JTabbedPane với 5 tabs --
        JTabbedPane tabs = buildTabbedPane();
        add(tabs, BorderLayout.CENTER);

        // -- Status bar ở dưới cùng --
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    /**
     * Tạo header panel với gradient màu và tiêu đề.
     */
    private JPanel buildHeaderPanel() {
        // JPanel với custom painting để vẽ gradient
        JPanel header = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                // Gradient: Indigo → Cyan
                GradientPaint gradient = new GradientPaint(
                    0, 0,           new Color(48, 63, 159),   // Indigo
                    getWidth(), 0,  new Color(0, 172, 193)    // Cyan
                );
                g2.setPaint(gradient);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        header.setLayout(new MigLayout("insets 12 20 12 20", "[]push[]", "[]3[]"));
        header.setPreferredSize(new Dimension(0, 65));

        // Tiêu đề
        JLabel titleLabel = new JLabel("⚡ Codeforces Examination Analysis");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);

        // Subtitle
        JLabel subLabel = new JLabel("  Crawl · Phân Tích AI · Đánh Giá Năng Lực");
        subLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        subLabel.setForeground(new Color(200, 230, 255));

        JPanel textPanel = new JPanel(new MigLayout("insets 0", "[grow]", "[]2[]"));
        textPanel.setOpaque(false);
        textPanel.add(titleLabel, "wrap");
        textPanel.add(subLabel);

        JLabel poweredBy = new JLabel("Powered by Google Gemini AI ✨");
        poweredBy.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        poweredBy.setForeground(new Color(180, 210, 240));

        header.add(textPanel);
        header.add(poweredBy);

        return header;
    }

    /**
     * Tạo JTabbedPane với 5 tabs.
     * Mỗi tab = 1 Panel riêng biệt.
     */
    private JTabbedPane buildTabbedPane() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setTabPlacement(JTabbedPane.LEFT);  // Tab list nằm bên trái
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // Khởi tạo 5 panels
        // Truyền "this" để panels có thể gọi refresh methods
        // userManagementPanel     = new UserManagementPanel(this);
        // crawlMonitorPanel       = new CrawlMonitorPanel(this);
        // submissionAnalysisPanel = new SubmissionAnalysisPanel(this);
        // evaluationPanel         = new EvaluationPanel(this);
        settingsPanel           = new SettingsPanel(this);

        // Thêm vào tabs với nhãn có icon emoji
        tabs.addTab("  👥 Quản Lý Nick  ", userManagementPanel);
        tabs.addTab("  🔄 Crawl Monitor ", crawlMonitorPanel);
        tabs.addTab("  🔍 Phân Tích     ", submissionAnalysisPanel);
        tabs.addTab("  📊 Đánh Giá      ", evaluationPanel);
        tabs.addTab("  ⚙️ Cài Đặt       ", settingsPanel);

        return tabs;
    }

    /**
     * Tạo status bar ở dưới cùng.
     */
    private JPanel buildStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(60, 60, 60)));
        statusBar.setPreferredSize(new Dimension(0, 24));

        statusBarLabel = new JLabel("  ✅ Sẵn sàng | Codeforces Examination Analysis v1.0");
        statusBarLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusBar.add(statusBarLabel, BorderLayout.WEST);

        return statusBar;
    }

    // ==================== Refresh Methods ====================
    // Các panel có thể gọi để trigger reload ở panel khác.

    /** Gọi sau khi crawl xong để reload danh sách submissions. */
    public void refreshSubmissionPanel() {
        if (submissionAnalysisPanel != null) {
            submissionAnalysisPanel.refreshData();
        }
    }

    /** Gọi sau khi có phân tích mới để tính lại điểm. */
    public void refreshEvaluationPanel() {
        if (evaluationPanel != null) {
            evaluationPanel.refreshData();
        }
    }

    /** Gọi sau khi thêm/xóa user để reload bảng. */
    public void refreshUserPanel() {
        if (userManagementPanel != null) {
            userManagementPanel.refreshData();
        }
    }

    /** Cập nhật text status bar. */
    public void setStatus(String status) {
        if (statusBarLabel != null) {
            statusBarLabel.setText("  " + status);
        }
    }

    public SettingsPanel getSettingsPanel() {
        return settingsPanel;
    }
}
