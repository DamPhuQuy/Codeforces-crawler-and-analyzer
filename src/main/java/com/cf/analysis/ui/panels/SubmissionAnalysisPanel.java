package com.cf.analysis.ui.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.cf.analysis.model.analysis.Analysis;
import com.cf.analysis.model.submission.Submission;
import com.cf.analysis.model.user.User;
import com.cf.analysis.ui.MainFrame;
import com.cf.analysis.ui.controllers.SubmissionAnalysisController;
import com.cf.analysis.ui.controllers.UserManagementController;
import com.cf.analysis.ui.dialogs.SubmissionDetailDialog;

import net.miginfocom.swing.MigLayout;

public class SubmissionAnalysisPanel extends JPanel {

    private final SubmissionAnalysisController analysisController;
    private final UserManagementController userController;

    // ====== UI: Phần trái ======
    private JComboBox<String>  userComboBox;
    private JTable             submissionTable;
    private DefaultTableModel  submissionModel;

    // ====== UI: Phần giữa ======
    private RSyntaxTextArea    codeArea;
    private RTextScrollPane    codeScrollPane;

    // ====== UI: Phần phải ======
    private JLabel             aiStatusLabel;
    private JProgressBar       aiConfBar;
    private JPanel             indicatorsPanel;   // 6 tiêu chí
    private JLabel             dsLabel;
    private JLabel             algoLabel;
    private JLabel             complexityLabel;
    private JTextArea          explanationArea;

    // ====== Toolbar ======
    private JButton         analyzeSelectedBtn;
    private JButton         analyzeAllBtn;
    private JProgressBar    analysisProgress;

    private static final String[] SUB_COLS = { "#", "Bài Toán", "Ngôn Ngữ", "Kết Quả", "Thời Gian", "PT" };

    private final MainFrame         mainFrame;
    private final java.time.format.DateTimeFormatter  sdf = java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm");

    // Cache submissions đang hiển thị (để biết row nào = Submission nào)
    private List<Submission> currentSubmissions = new ArrayList<>();

    // ==================== Constructor ====================

    public SubmissionAnalysisPanel(MainFrame mainFrame, UserManagementController userController, SubmissionAnalysisController analysisController) {
        this.mainFrame = mainFrame;
        this.userController = userController;
        this.analysisController = analysisController;
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        initComponents();
        loadUserList();
    }

    // ==================== Build UI ====================

    private void initComponents() {
        add(buildToolbar(), BorderLayout.NORTH);

        // Split chính: trái vs (giữa+phải)
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            buildLeftPanel(),
            buildCodeAndResultPanel()
        );
        mainSplit.setDividerLocation(290);
        mainSplit.setResizeWeight(0.2);
        add(mainSplit, BorderLayout.CENTER);
    }

    /** Toolbar chọn nick, nút analyze. */
    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new MigLayout("insets 0", "[]5[pref]10[pref]10[pref]push[200!]", ""));

        bar.add(new JLabel("Nick:"));

        userComboBox = new JComboBox<>();
        userComboBox.setPreferredSize(new Dimension(150, 28));
        bar.add(userComboBox);

        JButton loadBtn = new JButton("Tải");
        loadBtn.setFocusPainted(false);
        bar.add(loadBtn);

        analyzeSelectedBtn = new JButton("Phân Tích Đã Chọn");
        analyzeSelectedBtn.setBackground(new Color(80, 80, 80));
        analyzeSelectedBtn.setForeground(Color.WHITE);
        analyzeSelectedBtn.setFocusPainted(false);
        analyzeSelectedBtn.setEnabled(false);
        bar.add(analyzeSelectedBtn);

        analyzeAllBtn = new JButton("Phân Tích Tất Cả Pending");
        analyzeAllBtn.setFocusPainted(false);
        bar.add(analyzeAllBtn);

        analysisProgress = new JProgressBar(0, 100);
        analysisProgress.setStringPainted(true);
        analysisProgress.setString("");
        bar.add(analysisProgress, "growx");

        // Events
        loadBtn.addActionListener(e -> {
            String h = (String) userComboBox.getSelectedItem();
            if (h != null) loadSubmissions(h);
        });
        analyzeSelectedBtn.addActionListener(e -> analyzeSelected());
        analyzeAllBtn.addActionListener(e -> analyzeAll());

        return bar;
    }

    /** Panel trái: bảng submissions. */
    private JPanel buildLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Submissions"));

        submissionModel = new DefaultTableModel(SUB_COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        submissionTable = new JTable(submissionModel);
        submissionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        submissionTable.setRowHeight(26);
        submissionTable.getTableHeader().setReorderingAllowed(false);

        // Renderer cho cột Verdict
        submissionTable.getColumnModel().getColumn(3).setCellRenderer(new VerdictRenderer());

        // Cột số
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(JLabel.CENTER);
        submissionTable.getColumnModel().getColumn(0).setCellRenderer(center);
        submissionTable.getColumnModel().getColumn(2).setCellRenderer(center);
        submissionTable.getColumnModel().getColumn(5).setCellRenderer(center);

        // Độ rộng cột
        int[] widths = { 35, 120, 48, 40, 55, 30 };
        for (int i = 0; i < widths.length; i++) {
            submissionTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // Chọn row → hiển thị code + analysis
        submissionTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = submissionTable.getSelectedRow();
                if (row >= 0 && row < currentSubmissions.size()) {
                    analyzeSelectedBtn.setEnabled(true);
                    displaySubmission(currentSubmissions.get(row));
                }
            }
        });

        // Double-click → mở dialog chi tiết
        submissionTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) openDetailDialog();
            }
        });

        panel.add(new JScrollPane(submissionTable), BorderLayout.CENTER);
        return panel;
    }

    /** Phần giữa (code) + phải (AI result). */
    private JSplitPane buildCodeAndResultPanel() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            buildCodePanel(),
            buildAiResultPanel()
        );
        split.setDividerLocation(560);
        split.setResizeWeight(0.62);
        return split;
    }

    /** Code viewer với RSyntaxTextArea. */
    private JPanel buildCodePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Source Code"));

        codeArea = new RSyntaxTextArea(30, 80);
        codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        codeArea.setEditable(false);
        codeArea.setCodeFoldingEnabled(true);
        codeArea.setAntiAliasingEnabled(true);
        codeArea.setFont(new Font("Arial", Font.PLAIN, 13));

        // Áp monokai theme
        try {
            Theme.load(getClass().getResourceAsStream(
                "/org/fife/ui/rsyntaxtextarea/themes/monokai.xml")
            ).apply(codeArea);
        } catch (Exception ignored) {}

        codeScrollPane = new RTextScrollPane(codeArea);
        codeScrollPane.setLineNumbersEnabled(true);

        panel.add(codeScrollPane, BorderLayout.CENTER);

        // Legend màu highlight
        panel.add(buildHighlightLegend(), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildHighlightLegend() {
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        legend.setBackground(new Color(28, 28, 35));
        legend.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(50, 50, 60)));

        addLegend(legend, "Comment",    new Color(255, 240, 50));
        addLegend(legend, "AI Pattern", new Color(255, 90, 90));
        addLegend(legend, "Naming",     new Color(80, 190, 255));
        addLegend(legend, "Too Clean",  new Color(190, 80, 255));
        addLegend(legend, "Style",      new Color(255, 150, 50));

        return legend;
    }

    private void addLegend(JPanel p, String text, Color color) {
        JPanel box = new JPanel();
        box.setBackground(color);
        box.setPreferredSize(new Dimension(12, 12));
        p.add(box);
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Arial", Font.PLAIN, 10));
        lbl.setForeground(new Color(180, 180, 180));
        p.add(lbl);
    }

    /** Panel phải: kết quả AI phân tích. */
    private JScrollPane buildAiResultPanel() {
        JPanel panel = new JPanel(new MigLayout("insets 10, wrap 1", "[grow, fill]", ""));

        // AI verdict
        aiStatusLabel = new JLabel("Chưa có phân tích AI");
        aiStatusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        aiStatusLabel.setForeground(Color.GRAY);
        panel.add(aiStatusLabel);

        // Confidence
        panel.add(new JLabel("Confidence:"));
        aiConfBar = new JProgressBar(0, 100);
        aiConfBar.setStringPainted(true);
        panel.add(aiConfBar, "growx");

        // 6 Tiêu chí
        panel.add(new JSeparator(), "gaptop 8");
        panel.add(new JLabel("6 Tiêu Chí Phát Hiện AI:"), "gaptop 4");

        indicatorsPanel = new JPanel(new MigLayout("insets 5, wrap 1", "[grow, fill]", "[]2[]"));
        indicatorsPanel.setBorder(BorderFactory.createLineBorder(new Color(55, 55, 65)));
        panel.add(indicatorsPanel, "growx");

        // CTDL
        panel.add(new JSeparator(), "gaptop 8");
        panel.add(new JLabel("Cấu Trúc Dữ Liệu:"), "gaptop 4");
        dsLabel = new JLabel("<html><i>Chưa phân tích</i></html>");
        dsLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        panel.add(dsLabel);

        // Thuật toán
        panel.add(new JLabel("Thuật Toán:"));
        algoLabel = new JLabel("<html><i>Chưa phân tích</i></html>");
        algoLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        panel.add(algoLabel);

        // Complexity
        panel.add(new JLabel("Độ Phức Tạp:"));
        complexityLabel = new JLabel("N/A");
        complexityLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        panel.add(complexityLabel);

        // Nhận xét
        panel.add(new JSeparator(), "gaptop 8");
        panel.add(new JLabel("Nhận Xét:"), "gaptop 4");
        explanationArea = new JTextArea();
        explanationArea.setEditable(false);
        explanationArea.setLineWrap(true);
        explanationArea.setWrapStyleWord(true);
        explanationArea.setFont(new Font("Arial", Font.PLAIN, 12));
        explanationArea.setBackground(new Color(35, 35, 45));
        JScrollPane explanScroll = new JScrollPane(explanationArea);
        explanScroll.setPreferredSize(new Dimension(0, 130));
        panel.add(explanScroll, "growx");

        return new JScrollPane(panel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }

    // ==================== Data Actions ====================

    /** Load danh sách nick vào ComboBox. */
    private void loadUserList() {
        analysisController.getAllUsers()
            .thenAccept(users -> {
                userComboBox.removeAllItems();
                for (User u : users) {
                    userComboBox.addItem(u.getHandle());
                }
            })
            .exceptionally(ex -> {
                // Ignore errors silently for combo box loading
                return null;
            });
    }

    /** Tải submissions của một user vào bảng trái. */
    private void loadSubmissions(String handle) {
        submissionModel.setRowCount(0);
        currentSubmissions.clear();
        analyzeSelectedBtn.setEnabled(false);

        analysisController.getSubmissionsByHandle(handle)
            .thenAccept(subs -> {
                currentSubmissions = subs;
                for (int i = 0; i < subs.size(); i++) {
                    Submission s = subs.get(i);
                    submissionModel.addRow(new Object[]{
                        i + 1,
                        s.getProblemName(),
                        s.getShortLanguage(),
                        "OK".equals(s.getVerdict()) ? "AC" : s.getVerdict(),
                        s.getSubmittedAt() != null ? s.getSubmittedAt().format(sdf) : "",
                        s.isAnalyzed() ? "[x]" : "[ ]"
                    });
                }
            })
            .exceptionally(ex -> {
                JOptionPane.showMessageDialog(mainFrame, "Lỗi tải submissions: " + ex.getMessage());
                return null;
            });
    }

    /** Hiển thị code và analysis khi chọn một row. */
    private void displaySubmission(Submission sub) {
        // Hiển thị source code
        String code = sub.getSourceCode();
        if (code == null || code.isBlank()) {
            codeArea.setText("// Không có source code cho submission này.\n// Có thể code bị ẩn hoặc chưa scrape được.");
            codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        } else {
            codeArea.setSyntaxEditingStyle(mapLanguage(sub.getLanguage()));
            codeArea.setText(code);
            codeArea.setCaretPosition(0);
        }

        // Clear highlight cũ
        codeArea.removeAllLineHighlights();

        // Load analysis (background vì query DB)
        analysisController.getAnalysis(sub.getId())
            .thenAccept(a -> {
                if (a != null) showAnalysis(a);
                else           clearAnalysis();
            })
            .exceptionally(ex -> {
                clearAnalysis();
                return null;
            });
    }

    /** Hiển thị kết quả AI phân tích và highlight code. */
    private void showAnalysis(Analysis analysis) {

        // AI Status
        float aiConf = analysis.getAiResult() != null ? analysis.getAiResult().getAiConfidence() : 0.0f;
        boolean ai = aiConf > 0.5f;
        String aiLabel = ai ? "PHÁT HIỆN DÙNG AI" : "KHÔNG PHÁT HIỆN DÙNG AI";
        aiStatusLabel.setText(aiLabel);
        aiStatusLabel.setForeground(ai ? new Color(200, 200, 200) : new Color(150, 150, 150));

        // Confidence
        int conf = (int)(aiConf * 100);
        aiConfBar.setValue(conf);
        aiConfBar.setString(conf + "%");
        aiConfBar.setForeground(new Color(120, 120, 120));

        // 6 tiêu chí
        indicatorsPanel.removeAll();
        if (analysis.getAiResult() != null && analysis.getAiResult().getAiIndicators() != null) {
            var ind = analysis.getAiResult().getAiIndicators();
            addIndicator("1. Code quá sạch",          ind.getTooClean().isDetected(),          ind.getTooClean().getEvidence());
            addIndicator("2. Comment sách giáo khoa", ind.getTextbookComments().isDetected(),  ind.getTextbookComments().getEvidence());
            addIndicator("3. Đặt tên hoàn hảo",       ind.getPerfectNaming().isDetected(),     ind.getPerfectNaming().getEvidence());
            addIndicator("4. Pattern giống AI",        ind.getAiPattern().isDetected(),         ind.getAiPattern().getEvidence());
            addIndicator("5. Không lỗi vặt",           ind.getTooPerfect().isDetected(),        ind.getTooPerfect().getEvidence());
            addIndicator("6. Style không giống CP",    ind.getWrongStyle().isDetected(),        ind.getWrongStyle().getEvidence());
        }
        indicatorsPanel.revalidate();
        indicatorsPanel.repaint();

        // CTDL & Algo
        List<String> ds = analysis.getComplexityAnalysis() != null ? analysis.getComplexityAnalysis().getDataStructures() : null;
        List<String> algos = analysis.getComplexityAnalysis() != null ? analysis.getComplexityAnalysis().getAlgorithms() : null;
        dsLabel.setText(ds    != null && !ds.isEmpty()    ? "<html>• " + String.join("<br>• ", ds)    + "</html>" : "<html><i>Không phát hiện</i></html>");
        algoLabel.setText(algos != null && !algos.isEmpty() ? "<html>• " + String.join("<br>• ", algos) + "</html>" : "<html><i>Không phát hiện</i></html>");

        String timeComp = analysis.getComplexityAnalysis() != null ? analysis.getComplexityAnalysis().getTimeComplexity() : "N/A";
        String spaceComp = analysis.getComplexityAnalysis() != null ? analysis.getComplexityAnalysis().getSpaceComplexity() : "N/A";
        int difficulty = analysis.getComplexityAnalysis() != null ? analysis.getComplexityAnalysis().getDifficultyScore() : 0;
        complexityLabel.setText("Time: " + timeComp + " | Space: " + spaceComp + " | Độ khó: " + difficulty + "/10");

        // Explanation
        String explanation = analysis.getAnalysisOutput() != null ? analysis.getAnalysisOutput().getExplanation() : "";
        explanationArea.setText(explanation);
        explanationArea.setCaretPosition(0);

        // Highlight dòng code
        applyHighlights(analysis);
    }

    private void addIndicator(String label, boolean detected, String evidence) {
        JPanel row = new JPanel(new MigLayout("insets 4 6 4 6", "[]8[grow]", ""));
        row.setBackground(detected ? new Color(80, 50, 50) : new Color(50, 80, 50));

        String status = detected ? "VI PHẠM" : "KHÔNG VI PHẠM";
        Color statusColor = detected ? new Color(255, 100, 100) : new Color(100, 200, 100);

        JLabel statusLabel = new JLabel(status);
        statusLabel.setForeground(statusColor);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 11));

        JLabel text = new JLabel("<html><b>" + label + "</b>"
                + (detected && evidence != null && !evidence.isBlank()
                   ? "<br><small style='color:#aaa'>" + evidence + "</small>" : "")
                + "</html>");
        text.setFont(new Font("Arial", Font.PLAIN, 11));

        row.add(statusLabel);
        row.add(text, "growx");
        indicatorsPanel.add(row, "growx");
    }

    private void applyHighlights(Analysis analysis) {
        // Highlighted lines feature removed from new Analysis model
    }

    private void clearAnalysis() {
        aiStatusLabel.setText("Chưa có phân tích AI");
        aiStatusLabel.setForeground(Color.GRAY);
        aiConfBar.setValue(0); aiConfBar.setString("");
        indicatorsPanel.removeAll(); indicatorsPanel.revalidate();
        dsLabel.setText("<html><i>Chưa phân tích</i></html>");
        algoLabel.setText("<html><i>Chưa phân tích</i></html>");
        complexityLabel.setText("N/A");
        explanationArea.setText("");
    }

    /** Phân tích submission đang chọn. */
    private void analyzeSelected() {
        int row = submissionTable.getSelectedRow();
        if (row < 0 || row >= currentSubmissions.size()) return;

        Submission sub = currentSubmissions.get(row);
        analyzeSelectedBtn.setEnabled(false);
        analysisProgress.setIndeterminate(true);
        analysisProgress.setString("Đang phân tích...");

        analysisController.analyzeSubmission(sub.getId(), null)
            .thenAccept(a -> {
                showAnalysis(a);
                submissionModel.setValueAt("[x]", row, 5);
                currentSubmissions.get(row).setAnalyzed(true);
                mainFrame.refreshEvaluationPanel();
                analyzeSelectedBtn.setEnabled(true);
                analysisProgress.setIndeterminate(false);
                analysisProgress.setString("");
            })
            .exceptionally(ex -> {
                JOptionPane.showMessageDialog(mainFrame, "Lỗi phân tích:\n" + ex.getMessage());
                analyzeSelectedBtn.setEnabled(true);
                analysisProgress.setIndeterminate(false);
                analysisProgress.setString("");
                return null;
            });
    }

    /** Phân tích tất cả submissions pending. */
    private void analyzeAll() {
        analyzeAllBtn.setEnabled(false);
        analysisProgress.setValue(0);
        analysisProgress.setString("0%");

        analysisController.analyzeAllPending(
            null, // logCallback (không cần trong panel này)
            (current, total) -> SwingUtilities.invokeLater(() -> {
                int pct = (int)((double) current / total * 100);
                analysisProgress.setValue(pct);
                analysisProgress.setString(current + "/" + total + " (" + pct + "%)");
                if (current >= total) {
                    analyzeAllBtn.setEnabled(true);
                    analysisProgress.setString("Hoàn tất!");
                    String h = (String) userComboBox.getSelectedItem();
                    if (h != null) loadSubmissions(h);
                    mainFrame.refreshEvaluationPanel();
                }
            })
        );
    }

    private void openDetailDialog() {
        int row = submissionTable.getSelectedRow();
        if (row < 0 || row >= currentSubmissions.size()) return;
        Submission sub = currentSubmissions.get(row);
        try {
            Analysis a = analysisController.getAnalysisSync(sub.getId());
            new SubmissionDetailDialog(mainFrame, sub, a).setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(mainFrame, "Lỗi: " + ex.getMessage());
        }
    }

    public void refreshData() { loadUserList(); }

    private Color getHighlightColor(String cat) {
        return switch (cat != null ? cat : "") {
            case "textbook_comments" -> new Color(255, 240, 50, 55);
            case "ai_pattern" -> new Color(255, 80,  80, 55);
            case "perfect_naming" -> new Color(80,  190, 255, 55);
            case "too_clean" -> new Color(190, 80,  255, 55);
            case "wrong_style" -> new Color(255, 150, 50, 55);
            default -> new Color(200, 200, 200, 40);
        };
    }

    private String mapLanguage(String lang) {
        if (lang == null) return SyntaxConstants.SYNTAX_STYLE_NONE;
        String l = lang.toLowerCase();
        if (l.contains("java"))   return SyntaxConstants.SYNTAX_STYLE_JAVA;
        if (l.contains("python")) return SyntaxConstants.SYNTAX_STYLE_PYTHON;
        if (l.contains("c++") || l.contains("g++") || l.contains("clang")) return SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS;
        if (l.contains("c#"))     return SyntaxConstants.SYNTAX_STYLE_CSHARP;
        if (l.contains("kotlin")) return SyntaxConstants.SYNTAX_STYLE_KOTLIN;
        if (l.contains("rust"))   return SyntaxConstants.SYNTAX_STYLE_RUST;
        if (l.contains("go"))     return SyntaxConstants.SYNTAX_STYLE_GO;
        return SyntaxConstants.SYNTAX_STYLE_NONE;
    }

    // ---- Custom Renders ----
    private static class VerdictRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean f, int r, int c) {
            super.getTableCellRendererComponent(t, v, sel, f, r, c);
            setHorizontalAlignment(JLabel.CENTER);
            if (!sel && v != null) {
                if ("AC".equals(v.toString())) setForeground(new Color(180, 180, 180));
                else setForeground(new Color(150, 150, 150));
            }
            return this;
        }
    }
}
