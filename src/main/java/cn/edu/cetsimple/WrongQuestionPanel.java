package cn.edu.cetsimple;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
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

public class WrongQuestionPanel extends JPanel {
    private final JTextField searchField = new JTextField(18);
    private final JLabel countLabel = Theme.mutedLabel("共 0 道错题");
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "题型", "题目", "正确答案", "错误原因"}, 0) {
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    public WrongQuestionPanel() {
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
        intro.add(Theme.pageTitle("整理错题与薄弱项"));
        intro.add(Theme.mutedLabel("这里用于核对和少量修正；批量资料统一交给 AI 学习管家。"));
        wrapper.add(intro, BorderLayout.NORTH);

        JPanel tools = Theme.card();
        tools.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));
        Theme.styleSearchField(searchField);
        searchField.addActionListener(event -> loadData());
        JButton search = new JButton("查询");
        Theme.stylePrimaryButton(search);
        search.addActionListener(event -> loadData());
        tools.add(new JLabel("题目关键词"));
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
        JButton add = new JButton("新增错题");
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
        String sql = "SELECT * FROM wrong_question WHERE content LIKE ? ORDER BY id DESC";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "%" + searchField.getText().trim() + "%");
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    tableModel.addRow(new Object[]{
                            result.getInt("id"), result.getString("type"),
                            result.getString("content"), result.getString("answer"),
                            result.getString("note") == null ? "" : result.getString("note")
                    });
                }
            }
            countLabel.setText("共 " + tableModel.getRowCount() + " 道错题");
        } catch (Exception exception) {
            showError(exception);
        }
    }

    private void showEditor(Integer id) {
        JComboBox<String> type = new JComboBox<>(new String[]{"听力", "阅读", "写作", "翻译"});
        JTextArea content = new JTextArea(4, 26);
        JTextArea answer = new JTextArea(3, 26);
        content.setLineWrap(true);
        answer.setLineWrap(true);
        JTextField note = new JTextField();
        if (id != null) {
            int row = table.convertRowIndexToModel(table.getSelectedRow());
            type.setSelectedItem(tableModel.getValueAt(row, 1));
            content.setText(String.valueOf(tableModel.getValueAt(row, 2)));
            answer.setText(String.valueOf(tableModel.getValueAt(row, 3)));
            note.setText(String.valueOf(tableModel.getValueAt(row, 4)));
        }

        JPanel form = new JPanel(new GridLayout(0, 1, 5, 5));
        form.add(new JLabel("题型"));
        form.add(type);
        form.add(new JLabel("题目内容"));
        form.add(new JScrollPane(content));
        form.add(new JLabel("正确答案"));
        form.add(new JScrollPane(answer));
        form.add(new JLabel("错误原因"));
        form.add(note);
        if (JOptionPane.showConfirmDialog(this, form, id == null ? "新增错题" : "修改错题",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        if (content.getText().isBlank() || answer.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "题目和正确答案不能为空。", "请补充信息", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = id == null
                ? "INSERT INTO wrong_question(type, content, answer, note) VALUES(?,?,?,?)"
                : "UPDATE wrong_question SET type=?, content=?, answer=?, note=? WHERE id=?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, String.valueOf(type.getSelectedItem()));
            statement.setString(2, content.getText().trim());
            statement.setString(3, answer.getText().trim());
            statement.setString(4, note.getText().trim());
            if (id != null) statement.setInt(5, id);
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
            int start = !rows.isEmpty() && CsvFiles.isHeader(rows.get(0), "题型") ? 1 : 0;
            if (rows.size() <= start) throw new IllegalArgumentException("CSV 中没有可导入的数据。");
            int imported = 0;
            try (Connection connection = Database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO wrong_question(type, content, answer, note) VALUES(?,?,?,?)")) {
                connection.setAutoCommit(false);
                try {
                    for (int index = start; index < rows.size(); index++) {
                        List<String> row = rows.get(index);
                        if (row.size() < 3 || row.get(1).isBlank() || row.get(2).isBlank()) {
                            throw new IllegalArgumentException("第 " + (index + 1) + " 行缺少题目或正确答案。");
                        }
                        String type = row.get(0).isBlank() ? "阅读" : row.get(0);
                        statement.setString(1, type);
                        statement.setString(2, row.get(1));
                        statement.setString(3, row.get(2));
                        statement.setString(4, row.size() > 3 ? row.get(3) : "");
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
            JOptionPane.showMessageDialog(this, "成功导入 " + imported + " 道错题。", "导入完成", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception exception) {
            showError(exception);
        }
    }

    private void saveTemplate() {
        CsvFiles.saveTemplate(this, "wrong-questions-template.csv", List.of(
                List.of("题型", "题目", "正确答案", "错误原因"),
                List.of("阅读", "示例题目内容", "示例正确答案", "同义替换识别错误")
        ));
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先选择一条记录。");
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        showEditor((Integer) tableModel.getValueAt(modelRow, 0));
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先选择一条记录。");
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "确定删除所选错题吗？", "确认删除",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        int modelRow = table.convertRowIndexToModel(row);
        int id = (Integer) tableModel.getValueAt(modelRow, 0);
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM wrong_question WHERE id=?")) {
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
