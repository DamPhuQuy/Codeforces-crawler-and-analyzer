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
import javax.swing.JComboBox;
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
    private JComboBox<String> userComboBox;
    private HorizontalBarChartPanel barChart;
    private JLabel badgeLabel;
    private JLabel levelLabel;
    private JLabel statsLabel;

    private List<UserScore> scores;

    private static final String[] COLS = {
        "#", "Handle", "Điểm CTDL", "Điểm Thuật Toán", "Không Dùng AI", "Tổng Điểm", "Level", "Huy Hiệu", "Tỷ Lệ Dùng AI"
    };

    private final MainFrame mainFrame;

    private void initComponents() {
        // ---- Header ----
        JPanel header = new JPanel(new MigLayout("insets 0", "[]push[]", ""));
        JLabel title = new JLabel("Bảng Xếp Hạng Năng Lực");
        title.setFont(new Font("Arial", Font.BOLD, 17));
        header.add(title);

        userComboBox = new JComboBox<>();
        userComboBox.addActionListener(e -> {
            String handle = (String) userComboBox.getSelectedItem();
            if (handle == null || handle.startsWith("--")) return;

            // Tìm trong bảng xếp hạng trước
            boolean found = false;
            if (scores != null) {
                for (int i = 0; i < scores.size(); i++) {
                    if (scores.get(i).getHandle().equals(handle)) {
                        rankTable.setRowSelectionInterval(i, i);
                        rankTable.scrollRectToVisible(rankTable.getCellRect(i, 0, true));
                        found = true;
                        break;
                    }
                }
            }

            // Nếu không có trong bảng xếp hạng (leaderboard), tải trực tiếp
            if (!found) {
                controller.getUserScore(handle).thenAccept(score -> {
                    if (score != null) {
                        javax.swing.SwingUtilities.invokeLater(() -> showUserDetail(score));
                    }
                });
            }
        });
        header.add(new JLabel("Chọn User:"), "gapleft 20");
        header.add(userComboBox, "w 150!");

        refreshBtn = new JButton("Tính Lại Điểm");
        refreshBtn.setFocusPainted(false);
        refreshBtn.addActionListener(e -> {
            refreshBtn.setEnabled(false);
            controller.recalculateAllScores()
                .thenRun(() -> javax.swing.SwingUtilities.invokeLater(this::loadData))
                .exceptionally(ex -> {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(mainFrame, "Lỗi tính lại điểm: " + ex.getMessage());
                        refreshBtn.setEnabled(true);
                    });
                    return null;
                });
        });
        header.add(refreshBtn);
        add(header, BorderLayout.NORTH);

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

        // Chọn row → cập nhật detail panel và sync combo box
        rankTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && scores != null) {
                int row = rankTable.getSelectedRow();
                if (row >= 0 && row < scores.size()) {
                    UserScore selected = scores.get(row);
                    showUserDetail(selected);

                    // Sync ComboBox (avoid recursive event)
                    if (!selected.getHandle().equals(userComboBox.getSelectedItem())) {
                        userComboBox.setSelectedItem(selected.getHandle());
                    }
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

        // Horizontal Bar chart
        barChart = new HorizontalBarChartPanel();
        barChart.setPreferredSize(new Dimension(0, 200));
        panel.add(barChart, "h 200!, growx");

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

        // Load all users for ComboBox
        controller.getAllUserHandles().thenAccept(handles -> {
            javax.swing.SwingUtilities.invokeLater(() -> {
                userComboBox.removeAllItems();
                userComboBox.addItem("-- Chọn Nick --");
                for (String h : handles) userComboBox.addItem(h);
            });
        });
    }

    private void showUserDetail(UserScore score) {
        barChart.setScores(score.getDsScore(), score.getAlgorithmScore(), score.getAiScore());
        badgeLabel.setText(score.getBadge());

        levelLabel.setText("Level: " + score.getLevel().name());
        levelLabel.setForeground(switch (score.getLevel()) {
            case EXPERT -> new Color(200, 200, 200);
            case ADVANCED -> new Color(180, 180, 180);
            case INTERMEDIATE -> new Color(160, 160, 160);
            default -> Color.GRAY;
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

        barChart.repaint();
    }

    public void refreshData() { loadData(); }

    static class HorizontalBarChartPanel extends JPanel {

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

            int w = getWidth();
            int h = getHeight();

            if (w <= 100 || h <= 60) return;

            // Margins
            int leftMargin = 150;
            int rightMargin = 60;
            int topMargin = 20;
            int bottomMargin = 20;

            int chartWidth = w - leftMargin - rightMargin;
            int barHeight = 35;
            int barSpacing = 15;

            String[] labels = { "Điểm CTDL", "Điểm Thuật Toán", "Điểm Không Dùng AI" };
            double[] values = { dsScore, algoScore, aiScore };
            Color[] colors = {
                new Color(100, 150, 200),
                new Color(150, 100, 200),
                new Color(100, 200, 150)
            };

            int startY = topMargin;

            // Draw bars
            for (int i = 0; i < 3; i++) {
                int y = startY + i * (barHeight + barSpacing);

                // Draw label
                g2.setColor(new Color(180, 180, 180));
                g2.setFont(new Font("Arial", Font.BOLD, 12));
                g2.drawString(labels[i], 10, y + barHeight / 2 + 5);

                // Draw background bar
                g2.setColor(new Color(50, 50, 60));
                g2.fillRoundRect(leftMargin, y, chartWidth, barHeight, 8, 8);

                // Draw value bar
                int barWidth = (int) (chartWidth * (values[i] / 100.0));
                g2.setColor(colors[i]);
                g2.fillRoundRect(leftMargin, y, barWidth, barHeight, 8, 8);

                // Draw border
                g2.setColor(new Color(80, 80, 90));
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(leftMargin, y, chartWidth, barHeight, 8, 8);

                // Draw value text
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 13));
                String valueText = String.format("%.1f", values[i]);
                FontMetrics fm = g2.getFontMetrics();
                int textWidth = fm.stringWidth(valueText);

                if (barWidth > textWidth + 10) {
                    // Inside bar
                    g2.drawString(valueText, leftMargin + barWidth - textWidth - 8, y + barHeight / 2 + 5);
                } else {
                    // Outside bar
                    g2.setColor(new Color(180, 180, 180));
                    g2.drawString(valueText, leftMargin + barWidth + 8, y + barHeight / 2 + 5);
                }
            }

            // Draw scale markers
            g2.setColor(new Color(90, 90, 100));
            g2.setFont(new Font("Arial", Font.PLAIN, 10));
            for (int i = 0; i <= 100; i += 25) {
                int x = leftMargin + (int) (chartWidth * (i / 100.0));
                int y = startY + 3 * (barHeight + barSpacing) + 5;
                g2.drawString(String.valueOf(i), x - 8, y);
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
                    if (pct >= 50) setForeground(new Color(200, 200, 200));
                    else if (pct >= 20) setForeground(new Color(180, 180, 180));
                    else setForeground(new Color(160, 160, 160));
                } catch (NumberFormatException ignored) {}
            }
            return this;
        }
    }
}
