package cn.edu.cetsimple;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Consumer;

public class TodayPanel extends JPanel {
    private final JLabel heroTitle = new JLabel();
    private final JLabel heroSubtitle = Theme.mutedLabel("");
    private final JLabel dueCount = valueLabel();
    private final JLabel streak = valueLabel();
    private final JLabel reviewed = valueLabel();
    private final JLabel accuracy = valueLabel();
    private final JLabel taskDetail = new JLabel();
    private final JLabel insight = new JLabel();
    private final JButton startReview = new JButton("开始今日学习");
    private final JTextField aiInput = new JTextField();
    private final DailyProgressRing progressRing = new DailyProgressRing();
    private Consumer<String> openAiPrompt = prompt -> {};
    private Runnable openAiImport = () -> {};

    public TodayPanel() {
        setLayout(new BorderLayout(0, 18));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 30, 30, 30));
        add(createHero(), BorderLayout.NORTH);
        add(createCenter(), BorderLayout.CENTER);
        add(createAiBar(), BorderLayout.SOUTH);
        refresh();
    }

    public void setAiActions(Consumer<String> promptAction, Runnable importAction) {
        openAiPrompt = promptAction == null ? prompt -> {} : promptAction;
        openAiImport = importAction == null ? () -> {} : importAction;
    }

    public void refresh() {
        try {
            Database.LearningProgress progress = Database.loadLearningProgress();
            Database.DashboardStats stats = Database.loadDashboardStats();
            int target = Database.getTargetScore();
            int due = progress.dueWords() + progress.dueWrong();
            int completed = progress.reviewedToday();
            dueCount.setText(String.valueOf(due));
            streak.setText(String.valueOf(progress.streakDays()));
            reviewed.setText(String.valueOf(completed));
            accuracy.setText(progress.reviewedWeek() == 0 ? "--" : progress.accuracyWeek() + "%");
            progressRing.setProgress(due + completed == 0 ? 100 : completed * 100 / (due + completed));
            heroTitle.setText(greeting() + "，今天继续向 " + target + " 分前进");
            if (stats.latestScore() == 0) {
                heroSubtitle.setText("完成第一次模拟成绩记录后，这里会展示与目标的距离。");
            } else {
                int gap = target - (int) Math.round(stats.latestScore());
                heroSubtitle.setText(gap <= 0 ? "最近一次成绩已经达到目标，继续巩固薄弱项。"
                        : "最近成绩距离目标还差 " + gap + " 分。今天完成到期复习，保持稳定积累。");
            }
            int minutes = Math.max(5, Math.min(45, due * 2));
            taskDetail.setText(due == 0
                    ? "今天没有到期内容，可以导入新资料或让 AI 生成计划。"
                    : progress.dueWords() + " 个词汇  ·  " + progress.dueWrong() + " 道错题  ·  预计 " + minutes + " 分钟");
            startReview.setEnabled(due > 0);
            insight.setText(createInsight(progress, stats));
        } catch (Exception exception) {
            heroTitle.setText("今天继续学习");
            heroSubtitle.setText("暂时无法读取学习进度。");
        }
    }

    private JPanel createHero() {
        JPanel hero = Theme.gradientPanel(new Color(220, 239, 255), new Color(241, 232, 255), 30);
        hero.setLayout(new BorderLayout(20, 0));
        hero.setBorder(BorderFactory.createEmptyBorder(25, 28, 25, 25));
        JPanel text = new JPanel(new GridLayout(0, 1, 0, 7));
        text.setOpaque(false);
        heroTitle.setForeground(Theme.DARK);
        heroTitle.setFont(heroTitle.getFont().deriveFont(Font.BOLD, 27f));
        text.add(heroTitle);
        text.add(heroSubtitle);
        hero.add(text, BorderLayout.CENTER);
        hero.add(progressRing, BorderLayout.EAST);
        return hero;
    }

    private JPanel createCenter() {
        JPanel center = new JPanel(new BorderLayout(0, 16));
        center.setOpaque(false);
        JPanel metrics = new JPanel(new GridLayout(1, 4, 14, 0));
        metrics.setOpaque(false);
        metrics.add(metricCard("待复习", dueCount, "项", Theme.PRIMARY));
        metrics.add(metricCard("连续学习", streak, "天", Theme.ACCENT));
        metrics.add(metricCard("今日完成", reviewed, "项", Theme.SUCCESS));
        metrics.add(metricCard("近七天掌握率", accuracy, "", new Color(175, 82, 222)));
        center.add(metrics, BorderLayout.NORTH);

        JPanel lower = new JPanel(new GridLayout(1, 2, 16, 0));
        lower.setOpaque(false);
        lower.add(createTaskCard());
        lower.add(createInsightCard());
        center.add(lower, BorderLayout.CENTER);
        return center;
    }

    private JPanel metricCard(String title, JLabel value, String unit, Color color) {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 10));
        JPanel caption = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        caption.setOpaque(false);
        JLabel dot = new JLabel("●");
        dot.setForeground(color);
        caption.add(dot);
        caption.add(Theme.mutedLabel(title));
        card.add(caption, BorderLayout.NORTH);
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.add(value, BorderLayout.CENTER);
        JLabel unitLabel = Theme.mutedLabel(unit);
        unitLabel.setVerticalAlignment(JLabel.BOTTOM);
        row.add(unitLabel, BorderLayout.EAST);
        card.add(row, BorderLayout.CENTER);
        return card;
    }

    private JPanel createTaskCard() {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 14));
        JLabel title = new JLabel("今日任务");
        title.setForeground(Theme.DARK);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 19f));
        card.add(title, BorderLayout.NORTH);
        taskDetail.setForeground(Theme.TEXT_GRAY);
        taskDetail.setFont(taskDetail.getFont().deriveFont(14f));
        card.add(taskDetail, BorderLayout.CENTER);
        Theme.stylePrimaryButton(startReview);
        startReview.addActionListener(event -> startReview());
        JPanel button = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        button.setOpaque(false);
        button.add(startReview);
        card.add(button, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createInsightCard() {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 14));
        JLabel title = new JLabel("学习反馈");
        title.setForeground(Theme.DARK);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 19f));
        card.add(title, BorderLayout.NORTH);
        insight.setVerticalAlignment(JLabel.TOP);
        insight.setForeground(Theme.TEXT_GRAY);
        insight.setFont(insight.getFont().deriveFont(14f));
        card.add(insight, BorderLayout.CENTER);
        JButton plan = new JButton("让 AI 安排七天计划");
        Theme.styleSecondaryButton(plan);
        plan.addActionListener(event -> openAiPrompt.accept("请根据我的学习数据安排未来七天复习计划。"));
        JPanel button = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        button.setOpaque(false);
        button.add(plan);
        card.add(button, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createAiBar() {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(10, 0));
        JLabel mark = new JLabel("AI");
        mark.setForeground(Theme.PRIMARY);
        mark.setFont(mark.getFont().deriveFont(Font.BOLD, 17f));
        card.add(mark, BorderLayout.WEST);
        aiInput.setBackground(new Color(247, 247, 249));
        aiInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 225, 230)),
                BorderFactory.createEmptyBorder(9, 12, 9, 12)));
        aiInput.putClientProperty("JComponent.roundRect", true);
        aiInput.putClientProperty("JTextField.placeholderText", "问学习管家，或输入“把目标改成550分”……");
        aiInput.addActionListener(event -> sendToAi());
        card.add(aiInput, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton upload = new JButton("导入资料");
        JButton send = new JButton("发送");
        Theme.styleSecondaryButton(upload);
        Theme.stylePrimaryButton(send);
        upload.addActionListener(event -> openAiImport.run());
        send.addActionListener(event -> sendToAi());
        actions.add(upload);
        actions.add(send);
        card.add(actions, BorderLayout.EAST);
        return card;
    }

    private void sendToAi() {
        String prompt = aiInput.getText().trim();
        if (prompt.isBlank()) return;
        aiInput.setText("");
        openAiPrompt.accept(prompt);
    }

    private void startReview() {
        try {
            List<Database.ReviewItem> items = Database.loadDueReviewItems(20);
            if (items.isEmpty()) {
                JOptionPane.showMessageDialog(this, "今天没有到期的复习内容。", "今日任务已完成", JOptionPane.INFORMATION_MESSAGE);
                refresh();
                return;
            }
            new ReviewSessionDialog(SwingUtilities.getWindowAncestor(this), items, this::refresh).setVisible(true);
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "无法开始复习", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String createInsight(Database.LearningProgress progress, Database.DashboardStats stats) {
        if (progress.reviewedWeek() == 0) {
            return "<html>还没有复习记录。完成第一组卡片后，系统会根据你的反馈自动安排下一次复习。</html>";
        }
        String focus = progress.dueWrong() > progress.dueWords() ? "错题复盘" : "核心词汇";
        return "<html>近七天完成 <b>" + progress.reviewedWeek() + "</b> 次复习，掌握率 <b>"
                + progress.accuracyWeek() + "%</b>。<br><br>今天建议优先处理：<b>" + focus + "</b>。</html>";
    }

    private String greeting() {
        int hour = LocalTime.now().getHour();
        if (hour < 11) return "早上好";
        if (hour < 18) return "下午好";
        return "晚上好";
    }

    private static JLabel valueLabel() {
        JLabel label = new JLabel("0");
        label.setForeground(Theme.DARK);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 29f));
        return label;
    }

    private static final class DailyProgressRing extends JComponent {
        private int progress;

        private DailyProgressRing() {
            setPreferredSize(new Dimension(94, 94));
            setMinimumSize(new Dimension(94, 94));
        }

        private void setProgress(int progress) {
            this.progress = Math.max(0, Math.min(100, progress));
            repaint();
        }

        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = Math.min(getWidth(), getHeight()) - 12;
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;
            g.setColor(new Color(255, 255, 255, 180));
            g.fillOval(x, y, size, size);
            g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(0, 122, 255, 34));
            g.drawArc(x + 5, y + 5, size - 10, size - 10, 90, -360);
            g.setColor(Theme.PRIMARY);
            g.drawArc(x + 5, y + 5, size - 10, size - 10, 90, -(progress * 360 / 100));
            g.setFont(getFont().deriveFont(Font.BOLD, 18f));
            String value = progress + "%";
            int valueWidth = g.getFontMetrics().stringWidth(value);
            g.setColor(Theme.DARK);
            g.drawString(value, (getWidth() - valueWidth) / 2, getHeight() / 2 + 2);
            g.setFont(getFont().deriveFont(Font.PLAIN, 11f));
            String caption = "今日进度";
            int captionWidth = g.getFontMetrics().stringWidth(caption);
            g.setColor(Theme.TEXT_GRAY);
            g.drawString(caption, (getWidth() - captionWidth) / 2, getHeight() / 2 + 19);
            g.dispose();
        }
    }
}
