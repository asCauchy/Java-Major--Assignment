package cn.edu.cetsimple;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Font;
import java.awt.GraphicsEnvironment;

public class Main {
    public static void main(String[] args) {
        try {
            FlatLightLaf.setup();
            UIManager.put("defaultFont", new Font(systemFont(), Font.PLAIN, 14));
            UIManager.put("Component.arc", 18);
            UIManager.put("Button.arc", 18);
            UIManager.put("TextComponent.arc", 14);
            UIManager.put("ScrollPane.arc", 18);
            UIManager.put("Table.arc", 18);
            UIManager.put("ScrollBar.width", 12);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.trackArc", 999);
            UIManager.put("Component.focusWidth", 1);
            UIManager.put("Component.innerFocusWidth", 0);
            UIManager.put("Component.focusColor", new java.awt.Color(0, 122, 255, 80));
            UIManager.put("TabbedPane.showTabSeparators", false);
            Database.createTables();
            SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String systemFont() {
        String[] available = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String preferred : new String[]{"SF Pro Display", "Segoe UI Variable", "Segoe UI", "PingFang SC"}) {
            for (String font : available) {
                if (preferred.equalsIgnoreCase(font)) return font;
            }
        }
        return Font.SANS_SERIF;
    }
}
