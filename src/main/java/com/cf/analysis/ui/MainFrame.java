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

import com.cf.analysis.bll.AnalysisService;
import com.cf.analysis.bll.CrawlService;
import com.cf.analysis.bll.EvaluationService;
import com.cf.analysis.bll.SettingsService;
import com.cf.analysis.bll.UserService;
import com.cf.analysis.crawler.CodeforcesApiCaller;
import com.cf.analysis.dal.AnalysisDAO;
import com.cf.analysis.dal.SubmissionDAO;
import com.cf.analysis.dal.UserDAO;
import com.cf.analysis.dal.UserScoreDAO;
import com.cf.analysis.db.DatabaseConnection;
import com.cf.analysis.ui.controllers.CrawlController;
import com.cf.analysis.ui.controllers.EvaluationController;
import com.cf.analysis.ui.controllers.SettingsController;
import com.cf.analysis.ui.controllers.SubmissionAnalysisController;
import com.cf.analysis.ui.controllers.UserManagementController;
import com.cf.analysis.ui.panels.CrawlPanel;
import com.cf.analysis.ui.panels.EvaluationPanel;
import com.cf.analysis.ui.panels.SettingsPanel;
import com.cf.analysis.ui.panels.SubmissionAnalysisPanel;
import com.cf.analysis.ui.panels.UserManagementPanel;
import com.google.gson.Gson;

import net.miginfocom.swing.MigLayout;
import okhttp3.OkHttpClient;

public class MainFrame extends JFrame {

    private UserManagementPanel userManagementPanel;
    private CrawlPanel crawlMonitorPanel;
    private SubmissionAnalysisPanel submissionAnalysisPanel;
    private EvaluationPanel evaluationPanel;
    private SettingsPanel settingsPanel;

    private JLabel statusBarLabel;

    public MainFrame() {
        setTitle("Codeforces Examination Analysis System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 700));
        setPreferredSize(new Dimension(1450, 850));

        initDatabase();

        buildUI();

        pack();
        setLocationRelativeTo(null);

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

    private void initDatabase() {
        try {
            new DatabaseConnection();
            System.out.println("Database da ket noi!");
        } catch (Exception e) {
            System.err.println("Chua ket noi duoc database: " + e.getMessage());
        }
    }

    private void buildUI() {
        setLayout(new BorderLayout());

        add(buildHeaderPanel(), BorderLayout.NORTH);

        JTabbedPane tabs = buildTabbedPane();
        add(tabs, BorderLayout.CENTER);

        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel buildHeaderPanel() {
        JPanel header = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gradient = new GradientPaint(
                    0, 0,           new Color(50, 50, 50),
                    getWidth(), 0,  new Color(70, 70, 70)
                );
                g2.setPaint(gradient);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        header.setLayout(new MigLayout("insets 12 20 12 20", "[]push[]", "[]3[]"));
        header.setPreferredSize(new Dimension(0, 65));

        JLabel titleLabel = new JLabel("Codeforces Examination Analysis");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);

        JLabel subLabel = new JLabel("Crawl - Phân Tích AI - Đánh Giá Năng Lực");
        subLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        subLabel.setForeground(new Color(180, 180, 180));

        JPanel textPanel = new JPanel(new MigLayout("insets 0", "[grow]", "[]2[]"));
        textPanel.setOpaque(false);
        textPanel.add(titleLabel, "wrap");
        textPanel.add(subLabel);

        JLabel poweredBy = new JLabel("Powered by Google Gemini AI");
        poweredBy.setFont(new Font("Arial", Font.PLAIN, 11));
        poweredBy.setForeground(new Color(160, 160, 160));

        header.add(textPanel);
        header.add(poweredBy);

        return header;
    }

    private JTabbedPane buildTabbedPane() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setTabPlacement(JTabbedPane.LEFT);
        tabs.setFont(new Font("Arial", Font.PLAIN, 13));

        // database init
        DatabaseConnection db = new DatabaseConnection();
        Gson gson = new Gson();
        OkHttpClient httpClient = new OkHttpClient();

        // DAOs
        UserDAO userDAO = new UserDAO(db);
        SubmissionDAO submissionDAO = new SubmissionDAO(db);
        AnalysisDAO analysisDAO = new AnalysisDAO(db, gson);
        UserScoreDAO userScoreDAO = new UserScoreDAO(db);

        // api
        CodeforcesApiCaller cfClient = new CodeforcesApiCaller(httpClient, gson);

        // Services
        SettingsService settingsService = new SettingsService();
        UserService userService = new UserService(userDAO, cfClient);
        AnalysisService analysisService = new AnalysisService(submissionDAO, analysisDAO, settingsService);
        CrawlService crawlService = new CrawlService(userDAO, submissionDAO, cfClient, settingsService);
        EvaluationService evaluationService = new EvaluationService(userDAO, userScoreDAO, submissionDAO, analysisDAO);

        // Controllers
        UserManagementController userController = new UserManagementController(userService);
        SubmissionAnalysisController analysisController = new SubmissionAnalysisController(userService, analysisService);
        SettingsController settingsController = new SettingsController(settingsService);
        EvaluationController evaluationController = new EvaluationController(evaluationService);
        CrawlController crawlController = new CrawlController(crawlService, settingsService);

        userManagementPanel = new UserManagementPanel(this, userController);
        crawlMonitorPanel = new CrawlPanel(this, crawlController, settingsController);
        submissionAnalysisPanel = new SubmissionAnalysisPanel(this, userController, analysisController);
        evaluationPanel = new EvaluationPanel(this, evaluationController);
        settingsPanel = new SettingsPanel(this);

        // Thêm vào tabs
        tabs.addTab("  Quản Lý Nick  ", userManagementPanel);
        tabs.addTab("  Crawl ", crawlMonitorPanel);
        tabs.addTab("  Phân Tích     ", submissionAnalysisPanel);
        tabs.addTab("  Đánh Giá      ", evaluationPanel);
        tabs.addTab("  Cài Đặt       ", settingsPanel);

        return tabs;
    }

    private JPanel buildStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(60, 60, 60)));
        statusBar.setPreferredSize(new Dimension(0, 24));

        statusBarLabel = new JLabel("  Sẵn sàng | Codeforces Examination Analysis v1.0");
        statusBarLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        statusBar.add(statusBarLabel, BorderLayout.WEST);

        return statusBar;
    }

    public void refreshSubmissionPanel() {
        if (submissionAnalysisPanel != null) {
            submissionAnalysisPanel.refreshData();
        }
    }

    public void refreshEvaluationPanel() {
        if (evaluationPanel != null) {
            evaluationPanel.refreshData();
        }
    }

    public void refreshUserPanel() {
        if (userManagementPanel != null) {
            userManagementPanel.refreshData();
        }
    }

    public void setStatus(String status) {
        if (statusBarLabel != null) {
            statusBarLabel.setText("  " + status);
        }
    }

    public SettingsPanel getSettingsPanel() {
        return settingsPanel;
    }
}
