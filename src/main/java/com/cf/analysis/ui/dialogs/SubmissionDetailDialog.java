package com.cf.analysis.ui.dialogs;

import com.cf.analysis.model.analysis.Analysis;
import com.cf.analysis.model.submission.Submission;

import net.miginfocom.swing.MigLayout;
import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;

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
        JLabel probLabel = new JLabel("📝 " + submission.getProblemName());
        probLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        probLabel.setForeground(Color.WHITE);

        // Ngôn ngữ
        JLabel langLabel = new JLabel("[" + submission.getShortLanguage() + "]");
        langLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        langLabel.setForeground(new Color(180, 200, 220));

        // Verdict
        boolean ac = "OK".equals(submission.getVerdict());
        JLabel verdictLabel = new JLabel(ac ? "✅ Accepted" : "❌ " + submission.getVerdict());
        verdictLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        verdictLabel.setForeground(ac ? new Color(0, 220, 120) : new Color(255, 80, 80));

        // Thời gian / bộ nhớ
        JLabel perfLabel = new JLabel(submission.getTimeMs() + "ms · " + submission.getMemoryKb() + "KB");
        perfLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        perfLabel.setForeground(new Color(140, 160, 180));

        header.add(probLabel);
        header.add(langLabel);
        header.add(verdictLabel);
        header.add(perfLabel);

        return header;
    }

    // ==================== Code Panel ====================

    private JPanel buildCodePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("📝 Source Code"));

        // RSyntaxTextArea với monokai theme
        RSyntaxTextArea codeArea = new RSyntaxTextArea();
        codeArea.setSyntaxEditingStyle(mapLanguage(submission.getLanguage()));
        codeArea.setEditable(false);
        codeArea.setCodeFoldingEnabled(true);
        codeArea.setAntiAliasingEnabled(true);
        codeArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 13));

        try {
            Theme.load(getClass().getResourceAsStream(
                "/org/fife/ui/rsyntaxtextarea/themes/monokai.xml"
            )).apply(codeArea);
        } catch (Exception ignored) {}

        String code = submission.getSourceCode();
        codeArea.setText(code != null && !code.isBlank() ? code : "// Không có source code cho submission này.");
        codeArea.setCaretPosition(0);

        // Highlight các dòng nghi ngờ (nếu có analysis)
        if (analysis != null && analysis.getHighlightedLines() != null) {
            for (Analysis.HighlightedLine hl : analysis.getHighlightedLines()) {
                try {
                    codeArea.addLineHighlight(hl.line - 1, getHighlightColor(hl.category));
                } catch (Exception ignored) {}
            }
        }

        RTextScrollPane scroll = new RTextScrollPane(codeArea);
        scroll.setLineNumbersEnabled(true);
        panel.add(scroll, BorderLayout.CENTER);

        // Legend màu highlight
        if (analysis != null && analysis.getHighlightedLines() != null
                && !analysis.getHighlightedLines().isEmpty()) {
            panel.add(buildColorLegend(), BorderLayout.SOUTH);
        }

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
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        return l;
    }

    // ==================== Analysis Panel ====================

    private JScrollPane buildAnalysisPanel() {
        JPanel panel = new JPanel(new MigLayout("insets 12, wrap 1", "[grow, fill]", ""));

        if (analysis == null) {
            JLabel noData = new JLabel("⚠️ Chưa có phân tích AI. Chọn submission và bấm Phân Tích.");
            noData.setForeground(Color.GRAY);
            panel.add(noData);
            return scrollWrap(panel);
        }

        // ---- AI Verdict ----
        String verdictText = analysis.isAiDetected() ? "🤖 PHÁT HIỆN DÙNG AI" : "✅ KHÔNG PHÁT HIỆN DÙNG AI";
        JLabel verdict = new JLabel(verdictText);
        verdict.setFont(new Font("Segoe UI", Font.BOLD, 15));
        verdict.setForeground(analysis.isAiDetected() ? new Color(255, 80, 80) : new Color(0, 210, 100));
        panel.add(verdict);

        // ---- Confidence bar ----
        int conf = (int)(analysis.getAiConfidence() * 100);
        JProgressBar confBar = new JProgressBar(0, 100);
        confBar.setValue(conf);
        confBar.setStringPainted(true);
        confBar.setString("Confidence: " + conf + "%");
        confBar.setForeground(conf >= 70 ? new Color(255, 80, 80)
                            : conf >= 40 ? new Color(255, 180, 0)
                            : new Color(0, 200, 100));
        panel.add(confBar, "growx, gapbottom 8");

        // ---- Highlighted lines ----
        List<Analysis.HighlightedLine> lines = analysis.getHighlightedLines();
        if (lines != null && !lines.isEmpty()) {
            panel.add(new JSeparator());
            panel.add(new JLabel("🎯 Dòng code nghi ngờ:"));
            for (Analysis.HighlightedLine hl : lines) {
                JLabel lbl = new JLabel("  • Dòng " + hl.line + ": " + hl.reason);
                lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                lbl.setForeground(getTextColor(hl.category));
                panel.add(lbl);
            }
        }

        // ---- 6 tiêu chí ----
        if (analysis.getAiIndicators() != null) {
            panel.add(new JSeparator(), "gaptop 8");
            panel.add(new JLabel("🔍 6 Tiêu Chí Đánh Giá:"));

            Analysis.AiIndicators ind = analysis.getAiIndicators();
            addIndicatorRow(panel, "1. Code quá sạch",         ind.tooClean,           ind.tooCleanEvidence);
            addIndicatorRow(panel, "2. Comment sách giáo khoa",ind.textbookComments,   ind.textbookCommentsEvidence);
            addIndicatorRow(panel, "3. Đặt tên hoàn hảo",     ind.perfectNaming,      ind.perfectNamingEvidence);
            addIndicatorRow(panel, "4. Pattern giống AI",      ind.aiPattern,          ind.aiPatternEvidence);
            addIndicatorRow(panel, "5. Không lỗi vặt",         ind.tooPerfect,         ind.tooPerfectEvidence);
            addIndicatorRow(panel, "6. Style không giống CP",  ind.wrongStyle,         ind.wrongStyleEvidence);
        }

        // ---- CTDL & Algo ----
        panel.add(new JSeparator(), "gaptop 8");

        if (analysis.getDataStructures() != null && !analysis.getDataStructures().isEmpty()) {
            panel.add(new JLabel("📦 CTDL: " + String.join(", ", analysis.getDataStructures())));
        }
        if (analysis.getAlgorithms() != null && !analysis.getAlgorithms().isEmpty()) {
            panel.add(new JLabel("⚙️ Thuật toán: " + String.join(", ", analysis.getAlgorithms())));
        }
        panel.add(new JLabel("⏱ Time: " + analysis.getTimeComplexity()
                + " | Space: " + analysis.getSpaceComplexity()
                + " | Độ khó: " + analysis.getDifficultyScore() + "/10"));

        // ---- Explanation ----
        panel.add(new JSeparator(), "gaptop 8");
        panel.add(new JLabel("💬 Nhận Xét:"));

        JTextArea expArea = new JTextArea(analysis.getExplanation());
        expArea.setEditable(false);
        expArea.setLineWrap(true);
        expArea.setWrapStyleWord(true);
        expArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        expArea.setBackground(new Color(38, 38, 48));
        JScrollPane expScroll = new JScrollPane(expArea);
        expScroll.setPreferredSize(new Dimension(0, 120));
        panel.add(expScroll, "growx");

        return scrollWrap(panel);
    }

    private void addIndicatorRow(JPanel parent, String label, boolean detected, String evidence) {
        String icon  = detected ? "⚠️" : "✅";
        Color  color = detected ? new Color(255, 100, 100) : new Color(100, 220, 100);
        String html  = "<html>" + icon + " <b>" + label + "</b>"
                       + (detected && evidence != null && !evidence.isBlank()
                           ? " — <i>" + evidence + "</i>" : "")
                       + "</html>";
        JLabel lbl = new JLabel(html);
        lbl.setForeground(color);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
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
