package com.cf.analysis.ui.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import com.cf.analysis.bll.CrawlService;
import com.cf.analysis.bll.SettingsService;
import com.cf.analysis.ui.MainFrame;
import com.cf.analysis.ui.controllers.CrawlMonitorController;

import net.miginfocom.swing.MigLayout;

/**
 * Panel Tab 2: 🔄 Crawl Monitor.
 *
 * Chức năng:
 * - Nút "Crawl Tất Cả Ngay": bắt đầu crawl tất cả nick ngay
 * - Nút "Dừng": yêu cầu stop crawl giữa chừng
 * - Nút "Bật/Tắt Lịch Tự Động": crawl định kỳ mỗi X giờ
 * - Log console: real-time với màu sắc (xanh=OK, đỏ=lỗi, vàng=cảnh báo)
 * - Progress bar tổng thể
 *
 * Ghi chú SwingWorker cho người mới:
 * CrawlService tự tạo thread riêng và gọi callback.
 * Callback phải gọi SwingUtilities.invokeLater() trước khi update UI
 * vì Swing KHÔNG thread-safe (chỉ EDT mới được update UI).
 */
public class CrawlMonitorPanel extends JPanel {

    // ====== Services ======
    private final CrawlService crawlService;
    private final SettingsService settingsService;
    private final CrawlMonitorController controller;

    // ====== UI Components ======
    private JButton     crawlAllBtn;
    private JButton     stopBtn;
    private JButton     scheduleBtn;
    private JProgressBar progressBar;
    private JTextPane   logPane;       // JTextPane: hỗ trợ màu sắc khác nhau mỗi dòng
    private StyledDocument logDoc;    // Document của JTextPane để insert styled text
    private JLabel      scheduleStatusLabel;

    private boolean     scheduleActive = false;
    private final MainFrame mainFrame;

    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ==================== Constructor ====================

    public CrawlMonitorPanel(MainFrame mainFrame, CrawlService crawlService, SettingsService settingsService) {
        this.mainFrame = mainFrame;
        this.crawlService = crawlService;
        this.settingsService = settingsService;
        this.controller = new CrawlMonitorController(crawlService, settingsService);
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));

        initComponents();
        appendLog("═══════════════════════════════════════════════════", Color.DARK_GRAY);
        appendLog("  Codeforces Examination Analysis - Crawl Monitor", new Color(150, 150, 150));
        appendLog("  Nhấn 'Crawl Tất Cả Ngay' để bắt đầu.", new Color(150, 150, 150));
        appendLog("═══════════════════════════════════════════════════", Color.DARK_GRAY);
    }

    // ==================== Build UI ====================

    private void initComponents() {
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildLogPanel(), BorderLayout.CENTER);
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new MigLayout("insets 0", "[][]15[]push[]", "[]5[]"));

        // ---- Nút Crawl All ----
        crawlAllBtn = new JButton("Crawl Tất Cả Ngay");
        crawlAllBtn.setFont(new Font("Arial", Font.BOLD, 13));
        crawlAllBtn.setBackground(new Color(80, 80, 80));
        crawlAllBtn.setForeground(Color.WHITE);
        crawlAllBtn.setFocusPainted(false);
        crawlAllBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // ---- Nút Dừng ----
        stopBtn = new JButton("Dừng");
        stopBtn.setFont(new Font("Arial", Font.PLAIN, 13));
        stopBtn.setBackground(new Color(120, 120, 120));
        stopBtn.setForeground(Color.WHITE);
        stopBtn.setFocusPainted(false);
        stopBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        stopBtn.setEnabled(false);

        // ---- Nút Lịch tự động ----
        scheduleBtn = new JButton("Bật Lịch Tự Động");
        scheduleBtn.setFont(new Font("Arial", Font.PLAIN, 13));
        scheduleBtn.setFocusPainted(false);
        scheduleBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Label hiển thị lịch còn active không
        scheduleStatusLabel = new JLabel("Lịch: Chưa bật");
        scheduleStatusLabel.setForeground(Color.GRAY);
        scheduleStatusLabel.setFont(new Font("Arial", Font.ITALIC, 12));

        toolbar.add(crawlAllBtn);
        toolbar.add(stopBtn);
        toolbar.add(scheduleBtn);
        toolbar.add(scheduleStatusLabel, "wrap");

        // ---- Progress Bar ----
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Sẵn sàng");
        progressBar.setPreferredSize(new Dimension(0, 20));
        toolbar.add(progressBar, "span 5, growx");

        // ====== Event Listeners ======

        crawlAllBtn.addActionListener(e -> startCrawlAll());

        stopBtn.addActionListener(e -> {
            controller.stopCrawl();
            appendLog("Đã gửi yêu cầu dừng...", Color.LIGHT_GRAY);
            stopBtn.setEnabled(false);
        });

        scheduleBtn.addActionListener(e -> toggleSchedule());

        return toolbar;
    }

    /**
     * Tạo log console area.
     * Dùng JTextPane thay vì JTextArea để có thể tô màu từng dòng.
     */
    private JScrollPane buildLogPanel() {
        logPane = new JTextPane();
        logPane.setEditable(false);
        logPane.setBackground(new Color(18, 18, 22));
        logPane.setFont(new Font("Arial", Font.PLAIN, 12));
        logDoc = logPane.getStyledDocument();

        JScrollPane scroll = new JScrollPane(logPane);
        scroll.setBorder(BorderFactory.createTitledBorder("Log Console (Real-time)"));
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        return scroll;
    }

    // ==================== Actions ====================

    /**
     * Bắt đầu crawl tất cả users.
     *
     * CrawlService tự tạo background thread.
     * Ta truyền callback: khi service có log mới →
     * gọi SwingUtilities.invokeLater() để update UI trên EDT.
     */
    private void startCrawlAll() {
        crawlAllBtn.setEnabled(false);
        stopBtn.setEnabled(true);
        progressBar.setIndeterminate(true);
        progressBar.setString("Đang crawl...");
        appendLog("\n=== BẮT ĐẦU CRAWL (PARALLEL MODE) ===", new Color(180, 180, 180));
        appendLog("Sử dụng 5 threads song song với 1 browser instance", new Color(150, 150, 150));

        controller.crawlAllUsersAsync(
            // logCallback: CrawlService gọi callback này với mỗi dòng log
            // IMPORTANT: phải dùng invokeLater vì callback chạy trên background thread!
            message -> SwingUtilities.invokeLater(() -> {
                appendLog(message, getMessageColor(message));
                scrollToBottom(); // Auto scroll xuống dưới
            }),

            // doneCallback: gọi khi crawl hoàn tất
            totalNew -> SwingUtilities.invokeLater(() -> {
                progressBar.setIndeterminate(false);
                progressBar.setValue(100);
                progressBar.setString("Hoàn tất! +" + totalNew + " submissions mới");
                crawlAllBtn.setEnabled(true);
                stopBtn.setEnabled(false);

                // Refresh các panel khác
                mainFrame.refreshSubmissionPanel();
                mainFrame.refreshUserPanel();

                appendLog("=== CRAWL HOÀN TẤT: +" + totalNew + " submissions ===\n",
                          new Color(180, 180, 180));
            })
        );
    }

    /**
     * Bật/Tắt lịch crawl tự động.
     */
    private void toggleSchedule() {
        if (!scheduleActive) {
            // Bật lịch
            int hours = controller.getCrawlIntervalHours();
            controller.startSchedule(hours,
                msg -> SwingUtilities.invokeLater(() -> {
                    appendLog(msg, getMessageColor(msg));
                    scrollToBottom();
                })
            );

            scheduleActive = true;
            scheduleBtn.setText("Tắt Lịch Tự Động");
            scheduleBtn.setBackground(new Color(120, 120, 120));
            scheduleBtn.setForeground(Color.WHITE);
            scheduleStatusLabel.setText("Lịch: mỗi " + hours + "h | Đang hoạt động");
            scheduleStatusLabel.setForeground(new Color(150, 150, 150));

            appendLog("Lịch tự động đã bật (mỗi " + hours + " giờ)", new Color(150, 150, 150));

        } else {
            // Tắt lịch
            controller.stopSchedule();
            scheduleActive = false;
            scheduleBtn.setText("Bật Lịch Tự Động");
            scheduleBtn.setBackground(null);
            scheduleBtn.setForeground(null);
            scheduleStatusLabel.setText("Lịch: Chưa bật");
            scheduleStatusLabel.setForeground(Color.GRAY);

            appendLog("Đã tắt lịch tự động.", Color.LIGHT_GRAY);
        }
    }

    // ==================== Log Helpers ====================

    /**
     * Thêm một dòng log vào console với màu chỉ định.
     *
     * Cách hoạt động của JTextPane styled text:
     * 1. Tạo SimpleAttributeSet chứa thuộc tính (màu, font)
     * 2. insertString() thêm text vào Document với attributes đó
     */
    private void appendLog(String message, Color color) {
        try {
            SimpleAttributeSet attrs = new SimpleAttributeSet();
            StyleConstants.setForeground(attrs, color);
            StyleConstants.setFontFamily(attrs, "Consolas");
            StyleConstants.setFontSize(attrs, 12);

            // Thêm timestamp nếu message không phải line ngăn cách
            String text;
            if (message.startsWith("═") || message.startsWith("\n") || message.isBlank()) {
                text = message + "\n";
            } else {
                String ts = "[" + LocalDateTime.now().format(timeFormat) + "] ";
                SimpleAttributeSet tsAttrs = new SimpleAttributeSet();
                StyleConstants.setForeground(tsAttrs, new Color(80, 80, 80));
                StyleConstants.setFontFamily(tsAttrs, "Consolas");
                StyleConstants.setFontSize(tsAttrs, 12);
                logDoc.insertString(logDoc.getLength(), ts, tsAttrs);
                text = message + "\n";
            }

            logDoc.insertString(logDoc.getLength(), text, attrs);

        } catch (BadLocationException ignored) {}
    }

    /** Chọn màu log theo nội dung message. */
    private Color getMessageColor(String msg) {
        if (msg.contains("Hoàn tất") || msg.contains("thành công"))
            return new Color(100, 200, 100);
        if (msg.contains("Lỗi") || msg.contains("lỗi"))
            return new Color(255, 100, 100);
        if (msg.contains("Cảnh báo"))
            return new Color(255, 200, 100);
        if (msg.contains("==="))
            return new Color(160, 160, 200);
        if (msg.contains("Bắt đầu") || msg.contains("Khởi tạo") || msg.contains("Crawling"))
            return new Color(150, 180, 255);
        if (msg.contains("Đã đóng") || msg.contains("Crawled"))
            return new Color(180, 180, 180);
        return new Color(200, 200, 200);
    }

    /** Cuộn log pane xuống dưới cùng. */
    private void scrollToBottom() {
        logPane.setCaretPosition(logDoc.getLength());
    }

    /** Dừng tất cả task khi app đóng. */
    public void stopAll() {
        controller.stopCrawl();
        controller.stopSchedule();
    }
}
