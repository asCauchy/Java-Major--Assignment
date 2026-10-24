package cn.edu.cetsimple;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainFrame extends JFrame {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel content = new JPanel(cardLayout);
    private final JLabel pageTitle = Theme.pageTitle("今日学习");
    private final JLabel targetScoreLabel = new JLabel();
    private final Map<String, JButton> navigationButtons = new LinkedHashMap<>();
    private final DashboardPanel dashboardPanel = new DashboardPanel();
    private final AiAssistantPanel aiAssistantPanel = new AiAssistantPanel();
    private final TodayPanel todayPanel = new TodayPanel();

    public MainFrame() {
        setTitle("CET 四六级学习助手");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1240, 780);
        setMinimumSize(new Dimension(1020, 680));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        content.setBackground(Theme.BACKGROUND);
        content.add(todayPanel, "今日学习");
        content.add(aiAssistantPanel, "AI 学习管家");
        content.add(dashboardPanel, "数据概览");
        content.add(new WrongQuestionPanel(), "错题记录");
        content.add(new VocabularyPanel(), "词汇记录");
        content.add(new ScorePanel(), "成绩记录");

        add(createSidebar(), BorderLayout.WEST);
        add(createWorkspace(), BorderLayout.CENTER);
        aiAssistantPanel.setOnDataChanged(() -> {
            refreshTargetScore();
            dashboardPanel.refresh();
            todayPanel.refresh();
        });
        todayPanel.setAiActions(prompt -> {
            showPage("AI 学习管家");
            aiAssistantPanel.submitPrompt(prompt);
        }, () -> {
            showPage("AI 学习管家");
            aiAssistantPanel.openFilePicker();
        });
        refreshTargetScore();
        showPage("今日学习");
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(236, 0));
        sidebar.setBackground(Theme.SIDEBAR);
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER),
                BorderFactory.createEmptyBorder(30, 20, 24, 20)
        ));

        JPanel brand = new JPanel(new BorderLayout(12, 0));
        brand.setOpaque(false);
        JPanel mark = Theme.roundedPanel(Theme.PRIMARY, 18);
        mark.setPreferredSize(new Dimension(44, 44));
        mark.setLayout(new BorderLayout());
        JLabel letter = new JLabel("C", JLabel.CENTER);
        letter.setForeground(Color.WHITE);
        letter.setFont(letter.getFont().deriveFont(Font.BOLD, 22f));
        mark.add(letter);
        JPanel brandText = new JPanel();
        brandText.setOpaque(false);
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));
        JLabel logo = new JLabel("CET Focus");
        logo.setForeground(Theme.DARK);
        logo.setFont(logo.getFont().deriveFont(Font.BOLD, 20f));
        JLabel subtitle = new JLabel("四六级学习助手");
        subtitle.setForeground(Theme.TEXT_GRAY);
        subtitle.setFont(subtitle.getFont().deriveFont(12f));
        brandText.add(logo);
        brandText.add(Box.createVerticalStrut(3));
        brandText.add(subtitle);
        brand.add(mark, BorderLayout.WEST);
        brand.add(brandText, BorderLayout.CENTER);
        brand.setBorder(BorderFactory.createEmptyBorder(0, 4, 34, 0));
        sidebar.add(brand, BorderLayout.NORTH);

        JPanel menu = new JPanel();
        menu.setOpaque(false);
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        for (String name : new String[]{"今日学习", "AI 学习管家", "数据概览", "错题记录", "词汇记录", "成绩记录"}) {
            JButton button = createNavigationButton(name);
            navigationButtons.put(name, button);
            menu.add(button);
            menu.add(Box.createVerticalStrut(7));
        }
        sidebar.add(menu, BorderLayout.CENTER);

        JPanel privacy = Theme.roundedPanel(new Color(232, 232, 237), 18);
        privacy.setLayout(new BorderLayout(0, 4));
        privacy.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JLabel privacyTitle = new JLabel("本地数据保护");
        privacyTitle.setForeground(Theme.DARK);
        privacyTitle.setFont(privacyTitle.getFont().deriveFont(Font.BOLD, 12f));
        JLabel footer = new JLabel("资料保存在你的电脑中");
        footer.setForeground(Theme.TEXT_GRAY);
        footer.setFont(footer.getFont().deriveFont(11f));
        privacy.add(privacyTitle, BorderLayout.NORTH);
        privacy.add(footer, BorderLayout.SOUTH);
        sidebar.add(privacy, BorderLayout.SOUTH);
        return sidebar;
    }

    private JButton createNavigationButton(String name) {
        JButton button = new NavigationButton(name);
        button.setHorizontalAlignment(JButton.LEFT);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setRolloverEnabled(true);
        button.setForeground(Theme.TEXT_GRAY);
        button.setBackground(Theme.SIDEBAR);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 14f));
        button.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        button.setPreferredSize(new Dimension(196, 45));
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.addActionListener(event -> showPage(name));
        return button;
    }

    private static final class NavigationButton extends JButton {
        private NavigationButton(String text) {
            super(text);
            setContentAreaFilled(false);
        }

        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = getBackground();
            if (getModel().isRollover() && !Color.WHITE.equals(fill)) {
                fill = new Color(232, 232, 237);
            }
            g.setColor(fill);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            if (Color.WHITE.equals(getBackground())) {
                g.setColor(Theme.PRIMARY);
                g.fillRoundRect(5, 13, 4, Math.max(12, getHeight() - 26), 4, 4);
            }
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private JPanel createWorkspace() {
        JPanel workspace = new JPanel(new BorderLayout());
        workspace.setBackground(Theme.BACKGROUND);
        workspace.add(createTopBar(), BorderLayout.NORTH);
        workspace.add(content, BorderLayout.CENTER);
        return workspace;
    }

    private JPanel createTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Theme.BACKGROUND);
        topBar.setBorder(BorderFactory.createEmptyBorder(22, 30, 8, 30));
        topBar.add(pageTitle, BorderLayout.WEST);

        JPanel goalCard = Theme.gradientPanel(Color.WHITE, new Color(239, 246, 255), 22);
        goalCard.setLayout(new FlowLayout(FlowLayout.RIGHT, 12, 3));
        goalCard.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 8));
        JLabel label = new JLabel("我的目标");
        label.setForeground(Theme.TEXT_GRAY);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
        targetScoreLabel.setForeground(Theme.PRIMARY);
        targetScoreLabel.setFont(targetScoreLabel.getFont().deriveFont(Font.BOLD, 23f));
        JButton change = new JButton("修改");
        Theme.styleSecondaryButton(change);
        change.addActionListener(event -> changeTargetScore());
        goalCard.add(label);
        goalCard.add(targetScoreLabel);
        goalCard.add(change);
        topBar.add(goalCard, BorderLayout.EAST);
        return topBar;
    }

    private void showPage(String name) {
        cardLayout.show(content, name);
        pageTitle.setText(name);
        for (Map.Entry<String, JButton> entry : navigationButtons.entrySet()) {
            boolean selected = entry.getKey().equals(name);
            entry.getValue().setBackground(selected ? Color.WHITE : Theme.SIDEBAR);
            entry.getValue().setForeground(selected ? Theme.PRIMARY : Theme.TEXT_GRAY);
        }
        if ("数据概览".equals(name)) {
            dashboardPanel.refresh();
        }
        if ("AI 学习管家".equals(name)) aiAssistantPanel.refreshOverview();
        if ("今日学习".equals(name)) todayPanel.refresh();
    }

    private void refreshTargetScore() {
        try {
            targetScoreLabel.setText(Database.getTargetScore() + " 分");
        } catch (Exception exception) {
            targetScoreLabel.setText("500 分");
        }
    }

    private void changeTargetScore() {
        int current;
        try {
            current = Database.getTargetScore();
        } catch (Exception exception) {
            current = 500;
        }
        JSpinner score = new JSpinner(new SpinnerNumberModel(current, 0, 710, 5));
        score.setPreferredSize(new Dimension(180, 38));
        int choice = JOptionPane.showConfirmDialog(
                this, score, "设置我的目标分数", JOptionPane.OK_CANCEL_OPTION);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            Database.saveTargetScore((Integer) score.getValue());
            refreshTargetScore();
            dashboardPanel.refresh();
            todayPanel.refresh();
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "保存失败", JOptionPane.ERROR_MESSAGE);
        }
    }
}
