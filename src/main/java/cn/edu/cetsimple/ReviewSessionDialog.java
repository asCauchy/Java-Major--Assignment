package cn.edu.cetsimple;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.List;

public class ReviewSessionDialog extends JDialog {
    private final List<Database.ReviewItem> items;
    private final Runnable onComplete;
    private final JLabel category = Theme.mutedLabel("");
    private final JLabel progressText = Theme.mutedLabel("");
    private final JProgressBar progress = new JProgressBar();
    private final JTextArea front = textArea(22f, true);
    private final JTextArea back = textArea(16f, false);
    private final JButton reveal = new JButton("显示答案");
    private final JButton again = new JButton("不会");
    private final JButton hard = new JButton("模糊");
    private final JButton good = new JButton("掌握");
    private int index;

    public ReviewSessionDialog(Window owner, List<Database.ReviewItem> items, Runnable onComplete) {
        super(owner, "今日复习", ModalityType.APPLICATION_MODAL);
        this.items = items;
        this.onComplete = onComplete;
        setSize(760, 580);
        setMinimumSize(new Dimension(680, 520));
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        add(createHeader(), BorderLayout.NORTH);
        add(createCard(), BorderLayout.CENTER);
        add(createActions(), BorderLayout.SOUTH);
        showCurrent();
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel(new BorderLayout(16, 0));
        panel.setBackground(Theme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 12, 24));
        JLabel title = new JLabel("今日复习");
        title.setForeground(Theme.DARK);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        JPanel right = new JPanel(new GridLayout(0, 1, 0, 6));
        right.setOpaque(false);
        progressText.setHorizontalAlignment(JLabel.RIGHT);
        progress.setMinimum(0);
        progress.setMaximum(Math.max(1, items.size()));
        progress.setPreferredSize(new Dimension(220, 10));
        progress.setForeground(Theme.PRIMARY);
        progress.setBackground(new Color(229, 229, 234));
        right.add(progressText);
        right.add(progress);
        panel.add(title, BorderLayout.WEST);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    private JPanel createCard() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Theme.BACKGROUND);
        wrapper.setBorder(BorderFactory.createEmptyBorder(8, 24, 14, 24));
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 14));
        card.add(category, BorderLayout.NORTH);
        JPanel content = new JPanel(new GridLayout(2, 1, 0, 12));
        content.setOpaque(false);
        JScrollPane frontScroll = new JScrollPane(front);
        frontScroll.setBorder(BorderFactory.createEmptyBorder());
        frontScroll.getViewport().setBackground(Color.WHITE);
        JScrollPane backScroll = new JScrollPane(back);
        backScroll.setBorder(BorderFactory.createEmptyBorder());
        backScroll.getViewport().setBackground(new Color(249, 249, 251));
        content.add(frontScroll);
        content.add(backScroll);
        card.add(content, BorderLayout.CENTER);
        wrapper.add(card);
        return wrapper;
    }

    private JPanel createActions() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 24, 22, 24));
        Theme.stylePrimaryButton(reveal);
        reveal.addActionListener(event -> revealAnswer());
        JPanel ratings = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        ratings.setOpaque(false);
        Theme.styleDangerButton(again);
        Theme.styleSecondaryButton(hard);
        Theme.stylePrimaryButton(good);
        again.addActionListener(event -> rate("AGAIN"));
        hard.addActionListener(event -> rate("HARD"));
        good.addActionListener(event -> rate("GOOD"));
        ratings.add(again);
        ratings.add(hard);
        ratings.add(good);
        panel.add(reveal, BorderLayout.WEST);
        panel.add(ratings, BorderLayout.EAST);
        return panel;
    }

    private void showCurrent() {
        Database.ReviewItem item = items.get(index);
        category.setText(item.category() + "  ·  熟练度 " + item.familiarity() + "/5");
        progressText.setText((index + 1) + " / " + items.size());
        progress.setValue(index);
        front.setText(item.front());
        front.setCaretPosition(0);
        back.setText("先回想答案，然后点击“显示答案”。");
        back.setForeground(Theme.TEXT_GRAY);
        setRatingEnabled(false);
        reveal.setEnabled(true);
    }

    private void revealAnswer() {
        back.setText(items.get(index).back());
        back.setForeground(Theme.DARK);
        back.setCaretPosition(0);
        reveal.setEnabled(false);
        setRatingEnabled(true);
    }

    private void rate(String result) {
        try {
            Database.recordReview(items.get(index), result);
            index++;
            if (index >= items.size()) {
                progress.setValue(items.size());
                JOptionPane.showMessageDialog(this,
                        "今日复习完成，共处理 " + items.size() + " 项。",
                        "完成", JOptionPane.INFORMATION_MESSAGE);
                dispose();
                onComplete.run();
            } else {
                showCurrent();
            }
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "保存复习结果失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setRatingEnabled(boolean enabled) {
        again.setEnabled(enabled);
        hard.setEnabled(enabled);
        good.setEnabled(enabled);
    }

    private static JTextArea textArea(float size, boolean bold) {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBackground(bold ? Color.WHITE : new Color(249, 249, 251));
        area.setForeground(Theme.DARK);
        area.setFont(area.getFont().deriveFont(bold ? Font.BOLD : Font.PLAIN, size));
        area.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        return area;
    }
}
