package com.cf.analysis.ui.panels;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.cf.analysis.model.user.UserScore;
import com.cf.analysis.ui.MainFrame;
import com.cf.analysis.ui.controllers.EvaluationController;

import net.miginfocom.swing.MigLayout;

public class EvaluationPanel extends JPanel {

    private final EvaluationController controller;

    public EvaluationPanel(MainFrame mainFrame, EvaluationController evaluationController) {
        this.controller = evaluationController;
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        initComponents();
        loadData();
    }

    private JTable rankTable;
    private DefaultTableModel rankModel;
    private JButton refreshBtn;
    private RadarChartPanel radarChart;
    private JLabel badgeLabel;
    private JLabel levelLabel;
    private JLabel statsLabel;

    private List<UserScore> scores; // Cache để biết row nào = UserScore nào

    private static final String[] COLS = {
        "#", "Handle", "CTDL Score", "Algo Score", "AI-Free", "Overall", "Level", "Badge", "AI Usage"
    };

    private final MainFrame mainFrame;

    private void initComponents() {
        // ---- Header ----
        JPanel header = new JPanel(new MigLayout("insets 0", "[]push[]", ""));
        JLabel title = new JLabel("Bảng Xếp Hạng Năng Lực");
        title.setFont(new Font("Arial", Font.BOLD, 17));
        header.add(title);

        refreshBtn = new JButton("Tính Lại Điểm");
        refreshBtn.setFocusPainted(false);
        refreshBtn.addActionListener(e -> loadData());
        header.add(refreshBtn);
        add(header, BorderLayout.NORTH);

        // ---- Split: bảng vs chi tiết ----
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            buildRankingTable(),
            buildDetailPanel()
        );
        split.setDividerLocation(720);
        split.setResizeWeight(0.67);
        add(split, BorderLayout.CENTER);
    }

    private JScrollPane buildRankingTable() {
        rankModel = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        rankTable = new JTable(rankModel);
        rankTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rankTable.setRowHeight(32);
        rankTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        rankTable.getTableHeader().setReorderingAllowed(false);

        // Renderers
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(JLabel.CENTER);

        rankTable.getColumnModel().getColumn(0).setCellRenderer(center);
        rankTable.getColumnModel().getColumn(2).setCellRenderer(center);
        rankTable.getColumnModel().getColumn(3).setCellRenderer(center);
        rankTable.getColumnModel().getColumn(4).setCellRenderer(center);
        rankTable.getColumnModel().getColumn(5).setCellRenderer(new OverallScoreRenderer());
        rankTable.getColumnModel().getColumn(6).setCellRenderer(new LevelRenderer());
        rankTable.getColumnModel().getColumn(8).setCellRenderer(new AiUsageRenderer());

        // Độ rộng cột
        int[] widths = { 30, 100, 80, 80, 60, 65, 90, 130, 65 };
        for (int i = 0; i < widths.length; i++) {
            rankTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // Chọn row → cập nhật detail panel
        rankTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && scores != null) {
                int row = rankTable.getSelectedRow();
                if (row >= 0 && row < scores.size()) {
                    showUserDetail(scores.get(row));
                }
            }
        });

        JScrollPane scroll = new JScrollPane(rankTable);
        scroll.setBorder(BorderFactory.createTitledBorder("Xếp hạng tổng hợp (Nhấn vào user để xem radar chart)"));
        return scroll;
    }

    private JPanel buildDetailPanel() {
        JPanel panel = new JPanel(new MigLayout("insets 12, wrap 1", "[grow, fill]", ""));
        panel.setBorder(BorderFactory.createTitledBorder("Chi Tiết"));

        // Radar chart
        radarChart = new RadarChartPanel();
        radarChart.setPreferredSize(new Dimension(0, 280));
        panel.add(radarChart, "h 280!, growx");

        // Badge
        badgeLabel = new JLabel("Chọn user để xem chi tiết");
        badgeLabel.setFont(new Font("Arial", Font.BOLD, 16));
        badgeLabel.setHorizontalAlignment(JLabel.CENTER);
        panel.add(badgeLabel, "growx");

        // Level
        levelLabel = new JLabel("");
        levelLabel.setFont(new Font("Arial", Font.BOLD, 14));
        levelLabel.setHorizontalAlignment(JLabel.CENTER);
        panel.add(levelLabel, "growx");

        // Stats
        statsLabel = new JLabel("<html></html>");
        statsLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        panel.add(statsLabel, "growx, gaptop 8");

        return panel;
    }

    // ==================== Data ====================

    public void loadData() {
        refreshBtn.setEnabled(false);

        controller.getRanking()
            .thenAccept(userScores -> {
                scores = userScores;
                rankModel.setRowCount(0);
                for (int i = 0; i < scores.size(); i++) {
                    UserScore s = scores.get(i);
                    rankModel.addRow(new Object[]{
                        i + 1,
                        s.getHandle(),
                        String.format("%.0f", s.getDsScore()),
                        String.format("%.0f", s.getAlgorithmScore()),
                        String.format("%.0f", s.getAiScore()),
                        String.format("%.1f", s.getOverallScore()),
                        s.getLevel().name(),
                        s.getBadge(),
                        String.format("%.0f%%", s.getAiUsageRate() * 100)
                    });
                }
                refreshBtn.setEnabled(true);
            })
            .exceptionally(ex -> {
                JOptionPane.showMessageDialog(mainFrame, "Lỗi tải bảng xếp hạng: " + ex.getMessage());
                refreshBtn.setEnabled(true);
                return null;
            });
    }

    private void showUserDetail(UserScore score) {
        radarChart.setScores(score.getDsScore(), score.getAlgorithmScore(), score.getAiScore());
        badgeLabel.setText(score.getBadge());

        levelLabel.setText("Level: " + score.getLevel().name());
        levelLabel.setForeground(switch (score.getLevel()) {
            case EXPERT       -> new Color(200, 200, 200);
            case ADVANCED     -> new Color(180, 180, 180);
            case INTERMEDIATE -> new Color(160, 160, 160);
            default           -> Color.GRAY;
        });

        statsLabel.setText(String.format("""
            <html>
            <table cellspacing='4'>
            <tr><td><b>Handle:</b></td><td>%s</td></tr>
            <tr><td><b>Rating CF:</b></td><td>%d</td></tr>
            <tr><td><b>Submissions crawl:</b></td><td>%d (đã PT: %d)</td></tr>
            <tr><td><b>Tỷ lệ dùng AI:</b></td><td style='color:%s'>%.0f%% (%d submissions)</td></tr>
            <tr><td><b>CTDL nổi bật:</b></td><td>%s</td></tr>
            <tr><td><b>Thuật toán nổi bật:</b></td><td>%s</td></tr>
            </table></html>""",
            score.getHandle(),
            score.getRating(),
            score.getTotalSubmissions(), score.getAnalyzedSubmissions(),
            score.getAiUsageRate() >= 0.5 ? "red" : "green",
            score.getAiUsageRate() * 100, score.getAiDetectedCount(),
            score.getTopDataStructure(),
            score.getTopAlgorithm()
        ));

        radarChart.repaint();
    }

    public void refreshData() { loadData(); }

    // ==================== Radar Chart (Java2D) ====================

    /**
     * Vẽ radar chart với 3 trục: CTDL, Algorithm, AI-Free.
     * Sử dụng Java2D (Graphics2D) trực tiếp, không cần thư viện biểu đồ nào.
     */
    static class RadarChartPanel extends JPanel {

        private double dsScore   = 0;
        private double algoScore = 0;
        private double aiScore   = 0;

        public void setScores(double ds, double algo, double ai) {
            this.dsScore   = ds;
            this.algoScore = algo;
            this.aiScore   = ai;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w  = getWidth();
            int h  = getHeight();
            int cx = w / 2;
            int cy = h / 2;
            int r  = Math.min(w, h) / 2 - 50;
            if (r <= 0) return;

            // 3 trục: trên (CTDL), phải-dưới (Algo), trái-dưới (AI-Free)
            double[] angles = { Math.toRadians(-90), Math.toRadians(30), Math.toRadians(150) };
            String[] labels = { "CTDL", "Algorithm", "AI-Free" };
            double[] vals   = { dsScore / 100.0, algoScore / 100.0, aiScore / 100.0 };

            // ---- Vẽ lưới nền (3 vòng tam giác) ----
            g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0, new float[]{ 4, 4 }, 0));
            for (int ring = 1; ring <= 3; ring++) {
                int rr = r * ring / 3;
                int[] xp = new int[3], yp = new int[3];
                for (int i = 0; i < 3; i++) {
                    xp[i] = (int)(cx + rr * Math.cos(angles[i]));
                    yp[i] = (int)(cy + rr * Math.sin(angles[i]));
                }
                g2.setColor(new Color(70, 70, 80));
                g2.drawPolygon(xp, yp, 3);

                // Nhãn % trên trục đầu
                g2.setColor(new Color(90, 90, 100));
                g2.setFont(new Font("Arial", Font.PLAIN, 9));
                int lx = (int)(cx + (rr + 4) * Math.cos(angles[0]));
                int ly = (int)(cy + (rr + 4) * Math.sin(angles[0]));
                g2.drawString(ring * 33 + "%", lx + 3, ly);
            }

            // ---- Vẽ trục ----
            g2.setStroke(new BasicStroke(1));
            g2.setColor(new Color(90, 90, 100));
            for (double angle : angles) {
                g2.drawLine(cx, cy, (int)(cx + r * Math.cos(angle)), (int)(cy + r * Math.sin(angle)));
            }

            // ---- Vẽ vùng dữ liệu ----
            int[] dx = new int[3], dy = new int[3];
            for (int i = 0; i < 3; i++) {
                dx[i] = (int)(cx + r * vals[i] * Math.cos(angles[i]));
                dy[i] = (int)(cy + r * vals[i] * Math.sin(angles[i]));
            }

            // Fill bán trong suốt
            g2.setStroke(new BasicStroke(2));
            g2.setColor(new Color(100, 100, 100, 90));
            g2.fillPolygon(dx, dy, 3);
            g2.setColor(new Color(150, 150, 150, 200));
            g2.drawPolygon(dx, dy, 3);

            // Điểm dữ liệu
            g2.setColor(new Color(180, 180, 180));
            for (int i = 0; i < 3; i++) {
                g2.fillOval(dx[i] - 5, dy[i] - 5, 10, 10);
            }

            // ---- Nhãn trục ----
            g2.setFont(new Font("Arial", Font.BOLD, 12));
            g2.setColor(new Color(180, 180, 180));
            for (int i = 0; i < 3; i++) {
                int lx = (int)(cx + (r + 28) * Math.cos(angles[i]));
                int ly = (int)(cy + (r + 28) * Math.sin(angles[i]));
                FontMetrics fm = g2.getFontMetrics();

                // Hiện thị tên trục + điểm
                String scoreStr = String.format("%.0f", vals[i] * 100);
                String line1 = labels[i];
                String line2 = scoreStr + " pts";

                int tw1 = fm.stringWidth(line1);
                int tw2 = fm.stringWidth(line2);
                g2.drawString(line1, lx - tw1 / 2, ly - 2);

                g2.setColor(new Color(160, 160, 160));
                g2.setFont(new Font("Arial", Font.BOLD, 11));
                g2.drawString(line2, lx - tw2 / 2, ly + 13);

                g2.setColor(new Color(180, 180, 180));
                g2.setFont(new Font("Arial", Font.BOLD, 12));
            }
        }
    }

    // ==================== Custom Renderers ====================

    private static class OverallScoreRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean f, int r, int c) {
            super.getTableCellRendererComponent(t, v, sel, f, r, c);
            setHorizontalAlignment(JLabel.CENTER);
            setFont(new Font("Arial", Font.BOLD, 13));
            if (!sel && v != null) {
                try {
                    double score = Double.parseDouble(v.toString());
                    if      (score >= 75) setForeground(new Color(200, 200, 200));
                    else if (score >= 55) setForeground(new Color(180, 180, 180));
                    else if (score >= 35) setForeground(new Color(160, 160, 160));
                    else                  setForeground(Color.GRAY);
                } catch (NumberFormatException ignored) {}
            }
            return this;
        }
    }

    private static class LevelRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean f, int r, int c) {
            super.getTableCellRendererComponent(t, v, sel, f, r, c);
            setHorizontalAlignment(JLabel.CENTER);
            if (!sel && v != null) {
                switch (v.toString()) {
                    case "EXPERT"       -> setForeground(new Color(200, 200, 200));
                    case "ADVANCED"     -> setForeground(new Color(180, 180, 180));
                    case "INTERMEDIATE" -> setForeground(new Color(160, 160, 160));
                    default             -> setForeground(Color.GRAY);
                }
            }
            return this;
        }
    }

    private static class AiUsageRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean f, int r, int c) {
            super.getTableCellRendererComponent(t, v, sel, f, r, c);
            setHorizontalAlignment(JLabel.CENTER);
            if (!sel && v != null) {
                try {
                    double pct = Double.parseDouble(v.toString().replace("%",""));
                    if      (pct >= 50) setForeground(new Color(200, 200, 200));
                    else if (pct >= 20) setForeground(new Color(180, 180, 180));
                    else                setForeground(new Color(160, 160, 160));
                } catch (NumberFormatException ignored) {}
            }
            return this;
        }
    }
}
