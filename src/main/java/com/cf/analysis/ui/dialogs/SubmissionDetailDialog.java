package com.cf.analysis.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.cf.analysis.model.analysis.Analysis;
import com.cf.analysis.model.submission.Submission;

import net.miginfocom.swing.MigLayout;

/**
 * Dialog xem chi tiết một submission: source code đầy đủ + kết quả AI.
 * Mở khi double-click vào hàng trong bảng submissions.
 *
 * Layout:
 * - Header: tên bài + ngôn ngữ + verdict + thời gian
 * - Trái (60%): Source code với highlight
 * - Phải (40%): Kết quả phân tích AI
 */
public class SubmissionDetailDialog extends JDialog {

    private final Submission submission;
    private final Analysis   analysis;

    public SubmissionDetailDialog(JFrame parent, Submission submission, Analysis analysis) {
        super(parent, "Submission #" + submission.getSubmissionId() + " — " + submission.getProblemName(), false);
        this.submission = submission;
        this.analysis   = analysis;

        setPreferredSize(new Dimension(1150, 780));
        setLayout(new BorderLayout());

        buildUI();
        pack();
        setLocationRelativeTo(parent);
    }

    private void buildUI() {
        add(buildHeader(), BorderLayout.NORTH);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setDividerLocation(680);
        mainSplit.setResizeWeight(0.6);
        mainSplit.setLeftComponent(buildCodePanel());
        mainSplit.setRightComponent(buildAnalysisPanel());

        add(mainSplit, BorderLayout.CENTER);

        // Close button
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton closeBtn = new JButton("Đóng");
        closeBtn.setPreferredSize(new Dimension(90, 30));
        closeBtn.addActionListener(e -> dispose());
        footer.add(closeBtn);
        add(footer, BorderLayout.SOUTH);
    }

    // ==================== Header ====================

    private JPanel buildHeader() {
        JPanel header = new JPanel(new MigLayout("insets 10 15 10 15", "[]15[]15[]push[]", ""));
        header.setBackground(new Color(35, 35, 45));

        // Tên bài
        JLabel probLabel = new JLabel(submission.getProblemName());
        probLabel.setFont(new Font("Arial", Font.BOLD, 16));
        probLabel.setForeground(Color.WHITE);

        // Ngôn ngữ
        JLabel langLabel = new JLabel("[" + submission.getShortLanguage() + "]");
        langLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        langLabel.setForeground(new Color(180, 180, 180));

        // Verdict
        boolean ac = "OK".equals(submission.getVerdict());
        JLabel verdictLabel = new JLabel(ac ? "[OK] Accepted" : "[X] " + submission.getVerdict());
        verdictLabel.setFont(new Font("Arial", Font.BOLD, 13));
        verdictLabel.setForeground(ac ? new Color(180, 180, 180) : new Color(200, 200, 200));

        // Thời gian / bộ nhớ
        JLabel perfLabel = new JLabel(submission.getTimeMs() + "ms · " + submission.getMemoryKb() + "KB");
        perfLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        perfLabel.setForeground(new Color(160, 160, 160));

        header.add(probLabel);
        header.add(langLabel);
        header.add(verdictLabel);
        header.add(perfLabel);

        return header;
    }

    // ==================== Code Panel ====================

    private JPanel buildCodePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Source Code"));

        // RSyntaxTextArea với monokai theme
        RSyntaxTextArea codeArea = new RSyntaxTextArea();
        codeArea.setSyntaxEditingStyle(mapLanguage(submission.getLanguage()));
        codeArea.setEditable(false);
        codeArea.setCodeFoldingEnabled(true);
        codeArea.setAntiAliasingEnabled(true);
        codeArea.setFont(new Font("Arial", Font.PLAIN, 13));

        try {
            Theme.load(getClass().getResourceAsStream(
                "/org/fife/ui/rsyntaxtextarea/themes/monokai.xml"
            )).apply(codeArea);
        } catch (Exception ignored) {}

        String code = submission.getSourceCode();
        codeArea.setText(code != null && !code.isBlank() ? code : "// Không có source code cho submission này.");
        codeArea.setCaretPosition(0);

        RTextScrollPane scroll = new RTextScrollPane(codeArea);
        scroll.setLineNumbersEnabled(true);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildColorLegend() {
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        legend.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(60, 60, 60)));
        legend.setBackground(new Color(30, 30, 30));

        legend.add(makeLabel("Legend:", Color.GRAY));
        addLegendItem(legend, "Comment",    new Color(255, 230, 50));
        addLegendItem(legend, "AI Pattern", new Color(255, 100, 100));
        addLegendItem(legend, "Naming",     new Color(100, 200, 255));
        addLegendItem(legend, "Too Clean",  new Color(200, 100, 255));
        addLegendItem(legend, "Style",      new Color(255, 160, 50));

        return legend;
    }

    private void addLegendItem(JPanel parent, String text, Color color) {
        JPanel box = new JPanel();
        box.setBackground(color);
        box.setPreferredSize(new Dimension(14, 14));
        parent.add(box);
        parent.add(makeLabel(text, new Color(200, 200, 200)));
    }

    private JLabel makeLabel(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setForeground(color);
        l.setFont(new Font("Arial", Font.PLAIN, 11));
        return l;
    }

    // ==================== Analysis Panel ====================

    private JScrollPane buildAnalysisPanel() {
        JPanel panel = new JPanel(new MigLayout("insets 12, wrap 1", "[grow, fill]", ""));

        if (analysis == null) {
            JLabel noData = new JLabel("[!] Chưa có phân tích AI. Chọn submission và bấm Phân Tích.");
            noData.setForeground(Color.GRAY);
            panel.add(noData);
            return scrollWrap(panel);
        }

        // ---- AI Verdict ----
        float aiConf = analysis.getAiResult() != null ? analysis.getAiResult().getAiConfidence() : 0.0f;
        boolean aiDetected = aiConf > 0.5f;
        String verdictText = aiDetected ? "[AI] PHÁT HIỆN DÙNG AI" : "[OK] KHÔNG PHÁT HIỆN DÙNG AI";
        JLabel verdict = new JLabel(verdictText);
        verdict.setFont(new Font("Arial", Font.BOLD, 15));
        verdict.setForeground(aiDetected ? new Color(200, 200, 200) : new Color(180, 180, 180));
        panel.add(verdict);

        // ---- Confidence bar ----
        int conf = (int)(aiConf * 100);
        JProgressBar confBar = new JProgressBar(0, 100);
        confBar.setValue(conf);
        confBar.setStringPainted(true);
        confBar.setString("Confidence: " + conf + "%");
        confBar.setForeground(conf >= 70 ? new Color(200, 200, 200)
                            : conf >= 40 ? new Color(180, 180, 180)
                            : new Color(160, 160, 160));
        panel.add(confBar, "growx, gapbottom 8");

        // ---- 6 tiêu chí ----
        if (analysis.getAiResult() != null && analysis.getAiResult().getAiIndicators() != null) {
            panel.add(new JSeparator(), "gaptop 8");
            panel.add(new JLabel("6 Tiêu Chí Đánh Giá:"));

            var ind = analysis.getAiResult().getAiIndicators();
            addIndicatorRow(panel, "1. Code quá sạch",         ind.getTooClean().isDetected(),           ind.getTooClean().getEvidence());
            addIndicatorRow(panel, "2. Comment sách giáo khoa",ind.getTextbookComments().isDetected(),   ind.getTextbookComments().getEvidence());
            addIndicatorRow(panel, "3. Đặt tên hoàn hảo",     ind.getPerfectNaming().isDetected(),      ind.getPerfectNaming().getEvidence());
            addIndicatorRow(panel, "4. Pattern giống AI",      ind.getAiPattern().isDetected(),          ind.getAiPattern().getEvidence());
            addIndicatorRow(panel, "5. Không lỗi vặt",         ind.getTooPerfect().isDetected(),         ind.getTooPerfect().getEvidence());
            addIndicatorRow(panel, "6. Style không giống CP",  ind.getWrongStyle().isDetected(),         ind.getWrongStyle().getEvidence());
        }

        // ---- CTDL & Algo ----
        panel.add(new JSeparator(), "gaptop 8");

        List<String> ds = analysis.getComplexityAnalysis() != null ? analysis.getComplexityAnalysis().getDataStructures() : null;
        List<String> algos = analysis.getComplexityAnalysis() != null ? analysis.getComplexityAnalysis().getAlgorithms() : null;

        if (ds != null && !ds.isEmpty()) {
            panel.add(new JLabel("CTDL: " + String.join(", ", ds)));
        }
        if (algos != null && !algos.isEmpty()) {
            panel.add(new JLabel("Thuật toán: " + String.join(", ", algos)));
        }

        String timeComp = analysis.getComplexityAnalysis() != null ? analysis.getComplexityAnalysis().getTimeComplexity() : "N/A";
        String spaceComp = analysis.getComplexityAnalysis() != null ? analysis.getComplexityAnalysis().getSpaceComplexity() : "N/A";
        int difficulty = analysis.getComplexityAnalysis() != null ? analysis.getComplexityAnalysis().getDifficultyScore() : 0;
        panel.add(new JLabel("Time: " + timeComp + " | Space: " + spaceComp + " | Độ khó: " + difficulty + "/10"));

        // ---- Explanation ----
        panel.add(new JSeparator(), "gaptop 8");
        panel.add(new JLabel("Nhận Xét:"));

        String explanation = analysis.getAnalysisOutput() != null ? analysis.getAnalysisOutput().getExplanation() : "";
        JTextArea expArea = new JTextArea(explanation);
        expArea.setEditable(false);
        expArea.setLineWrap(true);
        expArea.setWrapStyleWord(true);
        expArea.setFont(new Font("Arial", Font.PLAIN, 12));
        expArea.setBackground(new Color(38, 38, 48));
        JScrollPane expScroll = new JScrollPane(expArea);
        expScroll.setPreferredSize(new Dimension(0, 120));
        panel.add(expScroll, "growx");

        return scrollWrap(panel);
    }

    private void addIndicatorRow(JPanel parent, String label, boolean detected, String evidence) {
        String icon  = detected ? "[!]" : "[OK]";
        Color  color = detected ? new Color(200, 200, 200) : new Color(160, 160, 160);
        String html  = "<html>" + icon + " <b>" + label + "</b>"
                       + (detected && evidence != null && !evidence.isBlank()
                           ? " — <i>" + evidence + "</i>" : "")
                       + "</html>";
        JLabel lbl = new JLabel(html);
        lbl.setForeground(color);
        lbl.setFont(new Font("Arial", Font.PLAIN, 12));
        parent.add(lbl);
    }

    // ==================== Util ====================

    private JScrollPane scrollWrap(JPanel panel) {
        return new JScrollPane(panel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }

    private Color getHighlightColor(String category) {
        return switch (category != null ? category : "") {
            case "textbook_comments" -> new Color(255, 240, 50, 55);
            case "ai_pattern"        -> new Color(255, 80,  80, 55);
            case "perfect_naming"    -> new Color(80,  180, 255, 55);
            case "too_clean"         -> new Color(180, 80,  255, 55);
            case "wrong_style"       -> new Color(255, 140, 50, 55);
            default                  -> new Color(200, 200, 200, 40);
        };
    }

    private Color getTextColor(String category) {
        return switch (category != null ? category : "") {
            case "textbook_comments" -> new Color(220, 200, 0);
            case "ai_pattern"        -> new Color(255, 100, 100);
            case "perfect_naming"    -> new Color(100, 200, 255);
            case "too_clean"         -> new Color(200, 100, 255);
            case "wrong_style"       -> new Color(255, 150, 50);
            default                  -> Color.GRAY;
        };
    }

    private String mapLanguage(String language) {
        if (language == null) return SyntaxConstants.SYNTAX_STYLE_NONE;
        String lower = language.toLowerCase();
        if (lower.contains("java"))                                       return SyntaxConstants.SYNTAX_STYLE_JAVA;
        if (lower.contains("python"))                                     return SyntaxConstants.SYNTAX_STYLE_PYTHON;
        if (lower.contains("c++") || lower.contains("g++") || lower.contains("clang")) return SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS;
        if (lower.contains("c#"))                                         return SyntaxConstants.SYNTAX_STYLE_CSHARP;
        if (lower.contains("kotlin"))                                     return SyntaxConstants.SYNTAX_STYLE_KOTLIN;
        if (lower.contains("rust"))                                       return SyntaxConstants.SYNTAX_STYLE_RUST;
        if (lower.contains("go"))                                         return SyntaxConstants.SYNTAX_STYLE_GO;
        return SyntaxConstants.SYNTAX_STYLE_NONE;
    }
}
