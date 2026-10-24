package cn.edu.cetsimple;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

public class DashboardPanel extends JPanel {
    private final JLabel wrongCount = valueLabel();
    private final JLabel wordCount = valueLabel();
    private final JLabel masteredCount = valueLabel();
    private final JLabel latestScore = valueLabel();
    private final JLabel progressText = new JLabel();
    private final JProgressBar progress = new JProgressBar(0, 710);

    public DashboardPanel() {
        setLayout(new BorderLayout(0, 20));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 30, 30, 30));
        add(createWelcome(), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 22));
        center.setOpaque(false);
        JPanel cards = new JPanel(new GridLayout(1, 4, 16, 0));
        cards.setOpaque(false);
        cards.add(statCard("累计错题", wrongCount, "道", new Color(255, 69, 58)));
        cards.add(statCard("词汇总量", wordCount, "个", Theme.PRIMARY));
        cards.add(statCard("已掌握词汇", masteredCount, "个", Theme.SUCCESS));
        cards.add(statCard("最近成绩", latestScore, "分", Theme.ACCENT));
        center.add(cards, BorderLayout.NORTH);
        center.add(createProgressCard(), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    public void refresh() {
        try {
            Database.DashboardStats stats = Database.loadDashboardStats();
            int target = Database.getTargetScore();
            wrongCount.setText(String.valueOf(stats.wrongCount()));
            wordCount.setText(String.valueOf(stats.wordCount()));
            masteredCount.setText(String.valueOf(stats.masteredCount()));
            latestScore.setText(stats.latestScore() == 0 ? "--" : formatScore(stats.latestScore()));
            int current = (int) Math.round(stats.latestScore());
            progress.setValue(Math.min(current, 710));
            progressText.setText(stats.latestScore() == 0
                    ? "录入一次模拟成绩后，这里会显示目标进度。"
                    : current >= target
                    ? "已达到当前目标，继续保持！"
                    : "距离目标还差 " + (target - current) + " 分");
        } catch (Exception exception) {
            progressText.setText("暂时无法读取学习统计。");
        }
    }

    private JPanel createWelcome() {
        JPanel panel = Theme.roundedPanel(new Color(229, 241, 255), 28);
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(24, 26, 24, 26));
        JPanel text = new JPanel(new GridLayout(0, 1, 0, 5));
        text.setOpaque(false);
        JLabel title = new JLabel("今天，离目标再近一点");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 27f));
        title.setForeground(Theme.DARK);
        text.add(title);
        text.add(Theme.mutedLabel("整理错题、积累词汇、记录成绩，让进步清晰可见。"));
        panel.add(text, BorderLayout.WEST);
        JPanel badgePanel = Theme.roundedPanel(Color.WHITE, 18);
        badgePanel.setLayout(new BorderLayout());
        badgePanel.setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));
        JLabel badge = new JLabel("保持专注", JLabel.CENTER);
        badge.setForeground(Theme.PRIMARY);
        badge.setFont(badge.getFont().deriveFont(Font.BOLD, 13f));
        badgePanel.add(badge, BorderLayout.CENTER);
        panel.add(badgePanel, BorderLayout.EAST);
        return panel;
    }

    private JPanel statCard(String title, JLabel value, String unit, Color tint) {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 14));
        JPanel caption = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        caption.setOpaque(false);
        JLabel dot = new JLabel("●");
        dot.setForeground(tint);
        dot.setFont(dot.getFont().deriveFont(11f));
        caption.add(dot);
        caption.add(Theme.mutedLabel(title));
        card.add(caption, BorderLayout.NORTH);
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.add(value, BorderLayout.CENTER);
        JLabel unitLabel = new JLabel(unit);
        unitLabel.setForeground(Theme.TEXT_GRAY);
        unitLabel.setVerticalAlignment(JLabel.BOTTOM);
        row.add(unitLabel, BorderLayout.EAST);
        card.add(row, BorderLayout.CENTER);
        return card;
    }

    private JPanel createProgressCard() {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 16));
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel title = new JLabel("目标进度");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setForeground(Theme.DARK);
        progressText.setForeground(Theme.TEXT_GRAY);
        heading.add(title, BorderLayout.WEST);
        heading.add(progressText, BorderLayout.EAST);
        card.add(heading, BorderLayout.NORTH);
        progress.setForeground(Theme.PRIMARY);
        progress.setBackground(new Color(229, 229, 234));
        progress.setPreferredSize(new java.awt.Dimension(0, 12));
        progress.putClientProperty("JProgressBar.largeHeight", true);
        JPanel body = new JPanel(new BorderLayout(0, 24));
        body.setOpaque(false);
        JPanel progressWrapper = new JPanel(new BorderLayout());
        progressWrapper.setOpaque(false);
        progressWrapper.add(progress, BorderLayout.NORTH);
        body.add(progressWrapper, BorderLayout.NORTH);

        JPanel steps = new JPanel(new GridLayout(1, 3, 14, 0));
        steps.setOpaque(false);
        steps.add(stepCard("01", "准备表格", "先下载 CSV 模板，在 Excel 中整理已有资料。"));
        steps.add(stepCard("02", "批量导入", "选择填写好的 CSV，一次加入多条学习记录。"));
        steps.add(stepCard("03", "持续复习", "通过搜索、掌握状态和成绩进度追踪提升。"));
        body.add(steps, BorderLayout.CENTER);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JPanel stepCard(String number, String title, String description) {
        JPanel panel = Theme.roundedPanel(Theme.SOFT_GRAY, 18);
        panel.setLayout(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(17, 17, 17, 17));
        JLabel numberLabel = new JLabel(number);
        numberLabel.setForeground(Theme.PRIMARY);
        numberLabel.setFont(numberLabel.getFont().deriveFont(Font.BOLD, 15f));
        panel.add(numberLabel, BorderLayout.NORTH);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Theme.DARK);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 17f));
        panel.add(titleLabel, BorderLayout.CENTER);
        JLabel descriptionLabel = new JLabel("<html><body style='width:170px'>" + description + "</body></html>");
        descriptionLabel.setForeground(Theme.TEXT_GRAY);
        panel.add(descriptionLabel, BorderLayout.SOUTH);
        return panel;
    }

    private static JLabel valueLabel() {
        JLabel label = new JLabel("0");
        label.setForeground(Theme.DARK);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 30f));
        return label;
    }

    private String formatScore(double value) {
        return value == Math.rint(value) ? String.format("%.0f", value) : String.format("%.1f", value);
    }
}
