package cn.edu.cetsimple;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.RenderingHints;

public class Theme {
    public static final Color BACKGROUND = new Color(245, 245, 247);
    public static final Color SURFACE = Color.WHITE;
    public static final Color PRIMARY = new Color(0, 122, 255);
    public static final Color PRIMARY_LIGHT = new Color(232, 243, 255);
    public static final Color DARK = new Color(29, 29, 31);
    public static final Color SIDEBAR = new Color(242, 242, 247);
    public static final Color ACCENT = new Color(255, 159, 10);
    public static final Color SUCCESS = new Color(52, 199, 89);
    public static final Color DANGER = new Color(255, 59, 48);
    public static final Color DANGER_LIGHT = new Color(255, 235, 234);
    public static final Color TEXT_GRAY = new Color(110, 110, 115);
    public static final Color BORDER = new Color(218, 218, 223);
    public static final Color SOFT_GRAY = new Color(242, 242, 247);

    public static JPanel card() {
        JPanel panel = roundedPanel(SURFACE, 22);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
        return panel;
    }

    public static JPanel roundedPanel(Color background, int radius) {
        return new RoundedPanel(background, radius);
    }

    public static JPanel gradientPanel(Color start, Color end, int radius) {
        return new GradientPanel(start, end, radius);
    }

    public static JLabel pageTitle(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(DARK);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 28f));
        return label;
    }

    public static JLabel mutedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_GRAY);
        label.setFont(label.getFont().deriveFont(13f));
        return label;
    }

    public static void stylePrimaryButton(JButton button) {
        styleButton(button, PRIMARY, Color.WHITE);
    }

    public static void styleDangerButton(JButton button) {
        styleButton(button, DANGER_LIGHT, DANGER);
    }

    public static void styleSecondaryButton(JButton button) {
        styleButton(button, SOFT_GRAY, DARK);
    }

    private static void styleButton(JButton button, Color background, Color foreground) {
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setRolloverEnabled(true);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 14f));
        button.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.putClientProperty("JButton.minimumWidth", 72);
        button.setUI(new ModernButtonUI());
    }

    public static void styleSearchField(JTextField field) {
        field.putClientProperty("JTextField.placeholderText", "输入关键词搜索");
        field.putClientProperty("JComponent.roundRect", true);
        field.setPreferredSize(new java.awt.Dimension(field.getPreferredSize().width, 40));
    }

    public static void styleTable(JTable table) {
        table.setRowHeight(44);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new java.awt.Dimension(0, 1));
        table.setGridColor(new Color(235, 235, 239));
        table.setSelectionBackground(PRIMARY_LIGHT);
        table.setSelectionForeground(DARK);
        table.setFont(table.getFont().deriveFont(14f));
        table.setBackground(SURFACE);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setBackground(SURFACE);
        table.getTableHeader().setForeground(DARK);
        table.getTableHeader().setFont(table.getFont().deriveFont(Font.BOLD, 14f));
        table.getTableHeader().setPreferredSize(new java.awt.Dimension(0, 44));
        table.getTableHeader().setReorderingAllowed(false);
    }

    private static final class RoundedPanel extends JPanel {
        private final Color fill;
        private final int radius;

        private RoundedPanel(Color fill, int radius) {
            this.fill = fill;
            this.radius = radius;
            setOpaque(false);
        }

        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0, 0, 0, 7));
            g.fillRoundRect(1, 3, Math.max(0, getWidth() - 2), Math.max(0, getHeight() - 4), radius, radius);
            g.setColor(fill);
            g.fillRoundRect(1, 1, Math.max(0, getWidth() - 2), Math.max(0, getHeight() - 3), radius, radius);
            g.dispose();
            super.paintComponent(graphics);
        }

        protected void paintBorder(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0, 0, 0, 13));
            g.drawRoundRect(1, 1, Math.max(0, getWidth() - 3), Math.max(0, getHeight() - 4), radius, radius);
            g.dispose();
        }
    }

    private static final class GradientPanel extends JPanel {
        private final Color start;
        private final Color end;
        private final int radius;

        private GradientPanel(Color start, Color end, int radius) {
            this.start = start;
            this.end = end;
            this.radius = radius;
            setOpaque(false);
        }

        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, start, getWidth(), getHeight(), end));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g.setColor(new Color(255, 255, 255, 80));
            g.drawRoundRect(1, 1, Math.max(0, getWidth() - 3), Math.max(0, getHeight() - 3), radius, radius);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class ModernButtonUI extends BasicButtonUI {
        public void paint(Graphics graphics, JComponent component) {
            JButton button = (JButton) component;
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = button.getBackground();
            if (!button.isEnabled()) {
                fill = new Color(232, 232, 237);
            } else if (button.getModel().isPressed()) {
                fill = mix(fill, Color.BLACK, 0.13f);
            } else if (button.getModel().isRollover()) {
                fill = mix(fill, Color.WHITE, 0.13f);
            }
            g.setColor(fill);
            g.fillRoundRect(0, 0, component.getWidth(), component.getHeight(), 16, 16);
            if (button.isEnabled() && fill.getRed() > 235 && fill.getGreen() > 235 && fill.getBlue() > 235) {
                g.setColor(new Color(0, 0, 0, 12));
                g.drawRoundRect(0, 0, Math.max(0, component.getWidth() - 1),
                        Math.max(0, component.getHeight() - 1), 16, 16);
            }
            g.dispose();
            super.paint(graphics, component);
        }
    }

    private static Color mix(Color first, Color second, float amount) {
        float keep = 1f - amount;
        return new Color(
                Math.min(255, Math.round(first.getRed() * keep + second.getRed() * amount)),
                Math.min(255, Math.round(first.getGreen() * keep + second.getGreen() * amount)),
                Math.min(255, Math.round(first.getBlue() * keep + second.getBlue() * amount))
        );
    }
}
