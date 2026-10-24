package cn.edu.cetsimple;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class VocabularyPanel extends JPanel {
    private final JTextField searchField = new JTextField(18);
    private final JLabel countLabel = Theme.mutedLabel("共 0 个词汇");
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "单词", "释义", "掌握状态"}, 0) {
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    public VocabularyPanel() {
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
        intro.add(Theme.pageTitle("建立自己的核心词库"));
        intro.add(Theme.mutedLabel("这里用于核对和少量修正；批量资料统一交给 AI 学习管家。"));
        wrapper.add(intro, BorderLayout.NORTH);

        JPanel tools = Theme.card();
        tools.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));
        Theme.styleSearchField(searchField);
        searchField.addActionListener(event -> loadData());
        JButton search = new JButton("查询");
        Theme.stylePrimaryButton(search);
        search.addActionListener(event -> loadData());
        tools.add(new JLabel("单词关键词"));
        tools.add(searchField);
        tools.add(search);
        tools.add(Theme.mutedLabel("批量导入请返回 AI 学习管家"));
        wrapper.add(tools, BorderLayout.SOUTH);
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
        JButton add = new JButton("新增词汇");
        JButton edit = new JButton("修改所选");
        JButton status = new JButton("切换掌握状态");
        JButton delete = new JButton("删除所选");
        Theme.stylePrimaryButton(add);
        Theme.styleSecondaryButton(edit);
        Theme.styleSecondaryButton(status);
        Theme.styleDangerButton(delete);
        add.addActionListener(event -> showEditor(null));
        edit.addActionListener(event -> editSelected());
        status.addActionListener(event -> changeStatus());
        delete.addActionListener(event -> deleteSelected());
        buttons.add(add);
        buttons.add(edit);
        buttons.add(status);
        buttons.add(delete);
        panel.add(buttons, BorderLayout.EAST);
        return panel;
    }

    private void loadData() {
        tableModel.setRowCount(0);
        String sql = "SELECT * FROM vocabulary WHERE word LIKE ? ORDER BY word";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "%" + searchField.getText().trim() + "%");
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    tableModel.addRow(new Object[]{
                            result.getInt("id"), result.getString("word"),
                            result.getString("meaning"), result.getString("status")
                    });
                }
            }
            countLabel.setText("共 " + tableModel.getRowCount() + " 个词汇");
        } catch (Exception exception) {
            showError(exception);
        }
    }

    private void showEditor(Integer id) {
        JTextField word = new JTextField();
        JTextField meaning = new JTextField();
        if (id != null) {
            int row = table.convertRowIndexToModel(table.getSelectedRow());
            word.setText(String.valueOf(tableModel.getValueAt(row, 1)));
            meaning.setText(String.valueOf(tableModel.getValueAt(row, 2)));
        }
        JPanel form = new JPanel(new GridLayout(0, 1, 5, 5));
        form.add(new JLabel("单词"));
        form.add(word);
        form.add(new JLabel("中文释义"));
        form.add(meaning);
        if (JOptionPane.showConfirmDialog(this, form, id == null ? "新增词汇" : "修改词汇",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        if (word.getText().isBlank() || meaning.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "单词和释义不能为空。", "请补充信息", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = id == null
                ? "INSERT INTO vocabulary(word, meaning) VALUES(?,?)"
                : "UPDATE vocabulary SET word=?, meaning=? WHERE id=?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, word.getText().trim());
            statement.setString(2, meaning.getText().trim());
            if (id != null) statement.setInt(3, id);
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
            int start = !rows.isEmpty() && CsvFiles.isHeader(rows.get(0), "单词") ? 1 : 0;
            if (rows.size() <= start) throw new IllegalArgumentException("CSV 中没有可导入的数据。");
            int imported = 0;
            try (Connection connection = Database.getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         INSERT INTO vocabulary(word, meaning, status) VALUES(?,?,?)
                         ON CONFLICT(word) DO UPDATE SET meaning=excluded.meaning, status=excluded.status
                         """)) {
                connection.setAutoCommit(false);
                try {
                    for (int index = start; index < rows.size(); index++) {
                        List<String> row = rows.get(index);
                        if (row.size() < 2 || row.get(0).isBlank() || row.get(1).isBlank()) {
                            throw new IllegalArgumentException("第 " + (index + 1) + " 行缺少单词或释义。");
                        }
                        String status = row.size() > 2 && "已掌握".equals(row.get(2)) ? "已掌握" : "未掌握";
                        statement.setString(1, row.get(0));
                        statement.setString(2, row.get(1));
                        statement.setString(3, status);
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
            JOptionPane.showMessageDialog(this, "成功导入或更新 " + imported + " 个词汇。",
                    "导入完成", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception exception) {
            showError(exception);
        }
    }

    private void saveTemplate() {
        CsvFiles.saveTemplate(this, "vocabulary-template.csv", List.of(
                List.of("单词", "释义", "状态"),
                List.of("significant", "重要的；显著的", "未掌握")
        ));
    }

    private int selectedModelRow() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "请先选择一个词汇。");
            return -1;
        }
        return table.convertRowIndexToModel(viewRow);
    }

    private void editSelected() {
        int row = selectedModelRow();
        if (row >= 0) showEditor((Integer) tableModel.getValueAt(row, 0));
    }

    private void changeStatus() {
        int row = selectedModelRow();
        if (row < 0) return;
        int id = (Integer) tableModel.getValueAt(row, 0);
        String oldStatus = String.valueOf(tableModel.getValueAt(row, 3));
        String newStatus = oldStatus.equals("已掌握") ? "未掌握" : "已掌握";
        executeUpdate("UPDATE vocabulary SET status=? WHERE id=?", newStatus, id);
    }

    private void deleteSelected() {
        int row = selectedModelRow();
        if (row < 0) return;
        if (JOptionPane.showConfirmDialog(this, "确定删除所选词汇吗？", "确认删除",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        executeUpdate("DELETE FROM vocabulary WHERE id=?", null, (Integer) tableModel.getValueAt(row, 0));
    }

    private void executeUpdate(String sql, String value, int id) {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (value == null) {
                statement.setInt(1, id);
            } else {
                statement.setString(1, value);
                statement.setInt(2, id);
            }
            statement.executeUpdate();
            loadData();
        } catch (Exception exception) {
            showError(exception);
        }
    }

    private void showError(Exception exception) {
        String message = exception.getMessage();
        if (message != null && message.contains("UNIQUE constraint failed")) message = "这个单词已经存在。";
        JOptionPane.showMessageDialog(this, message, "操作失败", JOptionPane.ERROR_MESSAGE);
    }
}
