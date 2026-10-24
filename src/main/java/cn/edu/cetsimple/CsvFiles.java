package cn.edu.cetsimple;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CsvFiles {
    public static Path chooseImportFile(Component parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择要导入的 CSV 文件");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV 表格文件 (*.csv)", "csv"));
        return chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION
                ? chooser.getSelectedFile().toPath()
                : null;
    }

    public static void saveTemplate(Component parent, String fileName, List<List<String>> rows) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("保存 CSV 导入模板");
        chooser.setSelectedFile(new java.io.File(fileName));
        chooser.setFileFilter(new FileNameExtensionFilter("CSV 表格文件 (*.csv)", "csv"));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path path = chooser.getSelectedFile().toPath();
        if (!path.getFileName().toString().toLowerCase().endsWith(".csv")) {
            path = path.resolveSibling(path.getFileName() + ".csv");
        }
        try {
            StringBuilder content = new StringBuilder("\uFEFF");
            for (List<String> row : rows) {
                for (int index = 0; index < row.size(); index++) {
                    if (index > 0) content.append(',');
                    content.append(escape(row.get(index)));
                }
                content.append("\r\n");
            }
            Files.writeString(path, content.toString(), StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(parent,
                    "模板已保存：\n" + path,
                    "保存成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(parent, exception.getMessage(), "保存失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static List<List<String>> read(Path path) throws IOException {
        String text = Files.readString(path, StandardCharsets.UTF_8);
        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
            text = text.substring(1);
        }
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;

        for (int index = 0; index < text.length(); index++) {
            char value = text.charAt(index);
            if (quoted) {
                if (value == '"') {
                    if (index + 1 < text.length() && text.charAt(index + 1) == '"') {
                        field.append('"');
                        index++;
                    } else {
                        quoted = false;
                    }
                } else {
                    field.append(value);
                }
            } else if (value == '"' && field.length() == 0) {
                quoted = true;
            } else if (value == ',') {
                row.add(field.toString().trim());
                field.setLength(0);
            } else if (value == '\n' || value == '\r') {
                if (value == '\r' && index + 1 < text.length() && text.charAt(index + 1) == '\n') {
                    index++;
                }
                row.add(field.toString().trim());
                field.setLength(0);
                if (!isEmpty(row)) rows.add(row);
                row = new ArrayList<>();
            } else {
                field.append(value);
            }
        }
        row.add(field.toString().trim());
        if (!isEmpty(row)) rows.add(row);
        if (quoted) throw new IOException("CSV 文件中存在未闭合的双引号。");
        return rows;
    }

    public static boolean isHeader(List<String> row, String firstColumn) {
        return !row.isEmpty() && row.get(0).replace(" ", "").equalsIgnoreCase(firstColumn.replace(" ", ""));
    }

    private static boolean isEmpty(List<String> row) {
        return row.stream().allMatch(String::isBlank);
    }

    private static String escape(String value) {
        String safe = value == null ? "" : value;
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            return '"' + safe.replace("\"", "\"\"") + '"';
        }
        return safe;
    }
}
