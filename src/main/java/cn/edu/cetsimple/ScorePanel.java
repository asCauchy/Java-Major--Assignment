package cn.edu.cetsimple;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

public class ScorePanel extends JPanel {
    private final JLabel countLabel = Theme.mutedLabel("共 0 次模拟记录");
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "考试日期", "听力", "阅读", "写作翻译", "总分"}, 0) {
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    public ScorePanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 28, 28));
        add(createHeader(), BorderLayout.NORTH);
        add(createTableCard(), BorderLayout.CENTER);
        add(createBottomBar(), BorderLayout.SOUTH);
        loadData();
    }

    private JPanel createHeader() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 14));
        wrapper.setOpaque(false);
        JPanel intro = new JPanel(new GridLayout(0, 1, 0, 4));
        intro.setOpaque(false);
        intro.add(Theme.pageTitle("记录每一次模拟考试"));
        intro.add(Theme.mutedLabel("这里用于核对和少量修正；批量成绩统一交给 AI 学习管家。"));
        wrapper.add(intro, BorderLayout.WEST);

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        tools.setOpaque(false);
        tools.add(Theme.mutedLabel("AI 可自动识别日期和各部分分数"));
        wrapper.add(tools, BorderLayout.EAST);
        return wrapper;
    }

    private JPanel createTableCard() {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout());
        Theme.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        card.add(scrollPane, BorderLayout.CENTER);
        return card;
    }

    private JPanel createBottomBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(countLabel, BorderLayout.WEST);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        JButton add = new JButton("新增成绩");
        JButton edit = new JButton("修改所选");
        JButton delete = new JButton("删除所选");
        Theme.stylePrimaryButton(add);
        Theme.styleSecondaryButton(edit);
        Theme.styleDangerButton(delete);
        add.addActionListener(event -> showEditor(null));
        edit.addActionListener(event -> editSelected());
        delete.addActionListener(event -> deleteSelected());
        buttons.add(add);
        buttons.add(edit);
        buttons.add(delete);
        panel.add(buttons, BorderLayout.EAST);
        return panel;
    }

    private void loadData() {
        tableModel.setRowCount(0);
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM exam_score ORDER BY exam_date DESC, id DESC");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                tableModel.addRow(new Object[]{
                        result.getInt("id"), result.getString("exam_date"),
                        result.getDouble("listening"), result.getDouble("reading"),
                        result.getDouble("writing"), result.getDouble("total")
                });
            }
            countLabel.setText("共 " + tableModel.getRowCount() + " 次模拟记录");
        } catch (Exception exception) {
            showError(exception);
        }
    }

    private void showEditor(Integer id) {
        JTextField date = new JTextField(LocalDate.now().toString());
        JSpinner listening = scoreSpinner(248.5);
        JSpinner reading = scoreSpinner(248.5);
        JSpinner writing = scoreSpinner(213.0);
        if (id != null) {
            int row = table.convertRowIndexToModel(table.getSelectedRow());
            date.setText(String.valueOf(tableModel.getValueAt(row, 1)));
            listening.setValue(tableModel.getValueAt(row, 2));
            reading.setValue(tableModel.getValueAt(row, 3));
            writing.setValue(tableModel.getValueAt(row, 4));
        }

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("考试日期（YYYY-MM-DD）"));
        form.add(date);
        form.add(new JLabel("听力（0～248.5）"));
        form.add(listening);
        form.add(new JLabel("阅读（0～248.5）"));
        form.add(reading);
        form.add(new JLabel("写作翻译（0～213）"));
        form.add(writing);
        if (JOptionPane.showConfirmDialog(this, form, id == null ? "新增模拟成绩" : "修改模拟成绩",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            LocalDate.parse(date.getText().trim());
        } catch (DateTimeParseException exception) {
            JOptionPane.showMessageDialog(this, "日期格式应为 YYYY-MM-DD，例如 2026-10-23。",
                    "日期格式错误", JOptionPane.WARNING_MESSAGE);
            return;
        }
        saveScore(id, date.getText().trim(), number(listening), number(reading), number(writing));
    }

    private void saveScore(Integer id, String date, double listening, double reading, double writing) {
        double total = listening + reading + writing;
        String sql = id == null
                ? "INSERT INTO exam_score(exam_date,listening,reading,writing,total) VALUES(?,?,?,?,?)"
                : "UPDATE exam_score SET exam_date=?,listening=?,reading=?,writing=?,total=? WHERE id=?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, date);
            statement.setDouble(2, listening);
            statement.setDouble(3, reading);
            statement.setDouble(4, writing);
            statement.setDouble(5, total);
            if (id != null) statement.setInt(6, id);
            statement.executeUpdate();
            loadData();
        } catch (Exception exception) {
            showError(exception);
        }
    }

    private void importCsv() {
        Path path = CsvFiles.chooseImportFile(this);
        if (path == null) return;
        try {
            List<List<String>> rows = CsvFiles.read(path);
            int start = !rows.isEmpty() && CsvFiles.isHeader(rows.get(0), "日期") ? 1 : 0;
            if (rows.size() <= start) throw new IllegalArgumentException("CSV 中没有可导入的数据。");
            int imported = 0;
            try (Connection connection = Database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO exam_score(exam_date,listening,reading,writing,total) VALUES(?,?,?,?,?)")) {
                connection.setAutoCommit(false);
                try {
                    for (int index = start; index < rows.size(); index++) {
                        List<String> row = rows.get(index);
                        if (row.size() < 4) throw new IllegalArgumentException("第 " + (index + 1) + " 行列数不足。");
                        LocalDate.parse(row.get(0));
                        double listening = parseScore(row.get(1), 248.5, index);
                        double reading = parseScore(row.get(2), 248.5, index);
                        double writing = parseScore(row.get(3), 213.0, index);
                        statement.setString(1, row.get(0));
                        statement.setDouble(2, listening);
                        statement.setDouble(3, reading);
                        statement.setDouble(4, writing);
                        statement.setDouble(5, listening + reading + writing);
                        statement.addBatch();
                        imported++;
                    }
                    statement.executeBatch();
                    connection.commit();
                } catch (Exception exception) {
                    connection.rollback();
                    throw exception;
                }
            }
            loadData();
            JOptionPane.showMessageDialog(this, "成功导入 " + imported + " 次模拟成绩。",
                    "导入完成", JOptionPane.INFORMATION_MESSAGE);
        } catch (DateTimeParseException exception) {
            showError(new IllegalArgumentException("CSV 中存在无效日期，请使用 YYYY-MM-DD 格式。"));
        } catch (Exception exception) {
            showError(exception);
        }
    }

    private double parseScore(String text, double maximum, int rowIndex) {
        try {
            double value = Double.parseDouble(text);
            if (value < 0 || value > maximum) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("第 " + (rowIndex + 1) + " 行存在超出范围的成绩。");
        }
    }

    private void saveTemplate() {
        CsvFiles.saveTemplate(this, "exam-scores-template.csv", List.of(
                List.of("日期", "听力", "阅读", "写作翻译"),
                List.of("2026-10-23", "180", "190", "150")
        ));
    }

    private JSpinner scoreSpinner(double maximum) {
        return new JSpinner(new SpinnerNumberModel(0.0, 0.0, maximum, 0.5));
    }

    private double number(JSpinner spinner) {
        return ((Number) spinner.getValue()).doubleValue();
    }

    private int selectedModelRow() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "请先选择一条成绩。");
            return -1;
        }
        return table.convertRowIndexToModel(viewRow);
    }

    private void editSelected() {
        int row = selectedModelRow();
        if (row >= 0) showEditor((Integer) tableModel.getValueAt(row, 0));
    }

    private void deleteSelected() {
        int row = selectedModelRow();
        if (row < 0) return;
        if (JOptionPane.showConfirmDialog(this, "确定删除所选成绩吗？", "确认删除",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        int id = (Integer) tableModel.getValueAt(row, 0);
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM exam_score WHERE id=?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
            loadData();
        } catch (Exception exception) {
            showError(exception);
        }
    }

    private void showError(Exception exception) {
        JOptionPane.showMessageDialog(this, exception.getMessage(), "操作失败", JOptionPane.ERROR_MESSAGE);
    }
}
