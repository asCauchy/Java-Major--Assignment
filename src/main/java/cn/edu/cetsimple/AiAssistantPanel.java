package cn.edu.cetsimple;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;

public class AiAssistantPanel extends JPanel {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int MAX_INPUT_LENGTH = 500_000;
    private final AiClient client = new AiClient();
    private final JComboBox<String> task = new JComboBox<>(new String[]{
            "自动识别并整理资料", "制定七天复习计划", "分析当前薄弱项", "自由询问学习管家"
    });
    private final JTextArea input = new JTextArea();
    private final JTextArea output = new JTextArea();
    private final JLabel providerStatus = Theme.mutedLabel("");
    private final JLabel runningStatus = Theme.mutedLabel("等待任务");
    private final JLabel fileStatus = Theme.mutedLabel("可选择多个 CSV、TXT 或 Markdown 文件");
    private final JLabel overview = Theme.mutedLabel("");
    private final JButton runButton = new JButton("交给 AI 处理");
    private final JButton saveButton = new JButton("确认导入全部数据");
    private JsonNode pendingImport;
    private Runnable dataChanged = () -> {};

    public AiAssistantPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 30, 30, 30));
        add(createHero(), BorderLayout.NORTH);
        add(createWorkspace(), BorderLayout.CENTER);
        refreshProviderStatus();
        refreshOverview();
    }

    public void refreshOverview() {
        try {
            Database.DashboardStats stats = Database.loadDashboardStats();
            overview.setText("本地资料  ·  " + stats.wrongCount() + " 道错题  ·  "
                    + stats.wordCount() + " 个词汇  ·  最近成绩 "
                    + (stats.latestScore() == 0 ? "暂无" : formatScore(stats.latestScore()) + " 分"));
        } catch (Exception exception) {
            overview.setText("本地学习数据暂时无法读取");
        }
    }

    public void setOnDataChanged(Runnable dataChanged) {
        this.dataChanged = dataChanged == null ? () -> {} : dataChanged;
    }

    public void openFilePicker() {
        task.setSelectedIndex(0);
        chooseFiles();
    }

    public void submitPrompt(String prompt) {
        input.setText(prompt == null ? "" : prompt.trim());
        if (input.getText().contains("七天") || input.getText().contains("计划")) {
            task.setSelectedIndex(1);
        } else if (input.getText().contains("目标") || input.getText().contains("掌握")
                || input.getText().contains("记录") || input.getText().contains("导入")) {
            task.setSelectedIndex(0);
        } else {
            task.setSelectedIndex(3);
        }
        runAiTask();
    }

    private JPanel createHero() {
        JPanel hero = Theme.gradientPanel(new Color(220, 239, 255), new Color(241, 232, 255), 30);
        hero.setLayout(new BorderLayout(20, 0));
        hero.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 22));
        JPanel text = new JPanel(new GridLayout(0, 1, 0, 5));
        text.setOpaque(false);
        JLabel title = new JLabel("今天想整理什么？");
        title.setForeground(Theme.DARK);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        JLabel subtitle = new JLabel("把资料交给 AI，它会自动识别、整理并准备写入数据库。");
        subtitle.setForeground(Theme.TEXT_GRAY);
        text.add(title);
        text.add(subtitle);
        text.add(overview);
        hero.add(text, BorderLayout.CENTER);
        JPanel settings = new JPanel(new GridLayout(0, 1, 0, 7));
        settings.setOpaque(false);
        providerStatus.setHorizontalAlignment(JLabel.RIGHT);
        JButton configure = new JButton("模型与密钥设置");
        Theme.styleSecondaryButton(configure);
        configure.addActionListener(event -> openSettings());
        settings.add(providerStatus);
        settings.add(configure);
        hero.add(settings, BorderLayout.EAST);
        return hero;
    }

    private JPanel createWorkspace() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 14));
        wrapper.setOpaque(false);
        wrapper.add(createQuickActions(), BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createInputCard(), createOutputCard());
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setResizeWeight(0.5);
        split.setDividerSize(14);
        split.setBackground(Theme.BACKGROUND);
        wrapper.add(split, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createQuickActions() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panel.setOpaque(false);
        panel.add(quickButton("智能导入资料", 0));
        panel.add(quickButton("生成七天计划", 1));
        panel.add(quickButton("分析薄弱项", 2));
        JLabel hint = Theme.mutedLabel("上传和批量整理统一从这里完成");
        hint.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        panel.add(hint);
        return panel;
    }

    private JButton quickButton(String text, int index) {
        JButton button = new JButton(text);
        Theme.styleSecondaryButton(button);
        button.addActionListener(event -> {
            task.setSelectedIndex(index);
            if (index > 0) runAiTask();
        });
        return button;
    }

    private JPanel createInputCard() {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 12));
        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setOpaque(false);
        task.setPreferredSize(new java.awt.Dimension(210, 40));
        toolbar.add(task, BorderLayout.CENTER);
        JButton upload = new JButton("选择资料文件");
        Theme.styleSecondaryButton(upload);
        upload.addActionListener(event -> chooseFiles());
        toolbar.add(upload, BorderLayout.EAST);
        card.add(toolbar, BorderLayout.NORTH);
        input.setLineWrap(true);
        input.setWrapStyleWord(true);
        input.setFont(input.getFont().deriveFont(14f));
        input.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        input.putClientProperty("JTextArea.placeholderText", "粘贴错题、单词、成绩或其他学习资料，也可以直接描述希望 AI 完成的事情……");
        JScrollPane scroll = new JScrollPane(input);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.putClientProperty("JComponent.roundRect", true);
        card.add(scroll, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(10, 0));
        footer.setOpaque(false);
        footer.add(fileStatus, BorderLayout.CENTER);
        Theme.stylePrimaryButton(runButton);
        runButton.addActionListener(event -> runAiTask());
        footer.add(runButton, BorderLayout.EAST);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createOutputCard() {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 12));
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        JLabel title = new JLabel("AI 处理结果");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 17f));
        title.setForeground(Theme.DARK);
        toolbar.add(title, BorderLayout.WEST);
        toolbar.add(runningStatus, BorderLayout.EAST);
        card.add(toolbar, BorderLayout.NORTH);
        output.setEditable(false);
        output.setLineWrap(true);
        output.setWrapStyleWord(true);
        output.setFont(output.getFont().deriveFont(14f));
        output.setBackground(new Color(249, 249, 251));
        output.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        output.setText("选择任务后，AI 会在这里给出整理结果或学习安排。\n\n批量数据会先显示预览，确认后才写入本地数据库。");
        JScrollPane scroll = new JScrollPane(output);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.putClientProperty("JComponent.roundRect", true);
        card.add(scroll, BorderLayout.CENTER);
        Theme.stylePrimaryButton(saveButton);
        saveButton.setEnabled(false);
        saveButton.addActionListener(event -> saveAllData());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        actions.add(saveButton);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private void chooseFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("把学习资料交给 AI");
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter("学习资料 (*.csv, *.txt, *.md)", "csv", "txt", "md"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        StringBuilder merged = new StringBuilder(input.getText().trim());
        int loaded = 0;
        try {
            for (java.io.File file : chooser.getSelectedFiles()) {
                String text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                if (!text.isEmpty() && text.charAt(0) == '\uFEFF') text = text.substring(1);
                if (merged.length() + text.length() > MAX_INPUT_LENGTH) {
                    throw new IllegalArgumentException("资料总长度过大，请分批上传。单次最多约 50 万个字符。");
                }
                if (!merged.isEmpty()) merged.append("\n\n");
                merged.append("===== 文件：").append(file.getName()).append(" =====\n").append(text);
                loaded++;
            }
            input.setText(merged.toString());
            input.setCaretPosition(0);
            task.setSelectedIndex(0);
            fileStatus.setText("已载入 " + loaded + " 个文件，点击“交给 AI 处理”即可自动分类");
        } catch (Exception exception) {
            showError(exception);
        }
    }

    private void openSettings() {
        AiSettingsDialog dialog = new AiSettingsDialog(SwingUtilities.getWindowAncestor(this), AiConfig.load());
        dialog.setVisible(true);
        if (dialog.getResult() != null) {
            AiConfig.save(dialog.getResult());
            refreshProviderStatus();
        }
    }

    private void refreshProviderStatus() {
        AiConfig config = AiConfig.load();
        providerStatus.setText(config.providerSummary());
        providerStatus.setForeground(config.isReady() ? Theme.SUCCESS : Theme.DANGER);
    }

    private void runAiTask() {
        AiConfig config = AiConfig.load();
        if (!config.isReady()) {
            JOptionPane.showMessageDialog(this, "请先选择模型并填写接口设置。", "尚未配置", JOptionPane.INFORMATION_MESSAGE);
            openSettings();
            return;
        }
        int selected = task.getSelectedIndex();
        if ((selected == 0 || selected == 3) && input.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "请先粘贴内容、选择文件或输入问题。", "缺少内容", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Prompt prompt;
        try {
            prompt = createPrompt(selected);
        } catch (Exception exception) {
            showError(exception);
            return;
        }
        setBusy(true);
        output.setText("AI 正在读取资料并完成任务，请稍候……");
        pendingImport = null;
        saveButton.setEnabled(false);
        new SwingWorker<String, Void>() {
            protected String doInBackground() throws Exception {
                return client.complete(config, prompt.system(), prompt.user());
            }
            protected void done() {
                try {
                    String response = get();
                    if (selected == 0) showImportPreview(response);
                    else output.setText(response.trim());
                    output.setCaretPosition(0);
                    runningStatus.setText("处理完成");
                } catch (Exception exception) {
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    showError(cause instanceof Exception e ? e : new Exception(cause));
                    runningStatus.setText("请求失败");
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private Prompt createPrompt(int selected) throws Exception {
        Database.DashboardStats stats = Database.loadDashboardStats();
        int target = Database.getTargetScore();
        String localData = "目标分数=" + target + "，最近成绩=" + stats.latestScore()
                + "，错题数量=" + stats.wrongCount() + "，词汇总量=" + stats.wordCount()
                + "，已掌握词汇=" + stats.masteredCount();
        if (selected == 0) {
            return new Prompt(
                    "你是大学英语四六级资料整理管家。自动判断输入中包含的错题、词汇和模拟成绩。"
                            + "只输出一个 JSON 对象，不要输出 Markdown 或解释。格式必须为："
                            + "{\"wrongQuestions\":[{\"type\":\"听力或阅读或写作或翻译\",\"content\":\"题目\",\"answer\":\"正确答案\",\"note\":\"解析\"}],"
                            + "\"vocabulary\":[{\"word\":\"英文单词\",\"meaning\":\"中文释义\",\"status\":\"未掌握或已掌握\"}],"
                            + "\"scores\":[{\"date\":\"YYYY-MM-DD\",\"listening\":0,\"reading\":0,\"writing\":0}],"
                            + "\"masteredWords\":[\"需要标记为已掌握的已有单词\"],\"targetScore\":null,"
                            + "\"message\":\"简短整理说明\"}。"
                            + "听力和阅读各不超过248.5，写作翻译不超过213。不要虚构缺失的题干、答案或成绩；"
                            + "如果用户明确要求修改目标分数，将 targetScore 填为0到710的整数，否则保持 null。"
                            + "无法确认的数据不要放入数组。去除明显重复项。今天日期是 " + LocalDate.now() + "。",
                    input.getText().trim()
            );
        }
        String user = localData + "。用户补充：" + input.getText().trim();
        if (selected == 1) {
            return new Prompt("你是大学英语四六级学习管家。根据统计数据制定未来七天计划。"
                    + "用中文输出，每天给出具体任务、建议时长和完成标准，内容简洁且可执行。没有提供的数据不要自行假设。", user);
        }
        if (selected == 2) {
            return new Prompt("你是大学英语四六级学习分析助手。根据统计信息判断最需要优先处理的薄弱项，"
                    + "给出三项按优先级排列的行动建议，并说明下一次模拟考试应重点观察什么。没有提供的数据要明确说明无法判断。", user);
        }
        return new Prompt("你是大学英语四六级学习管家。结合用户的本地学习统计回答问题，回答应简洁、具体、可执行。"
                + "不要声称读取了统计信息之外的数据。", user);
    }

    private void showImportPreview(String response) throws Exception {
        JsonNode root = JSON.readTree(extractJson(response));
        JsonNode wrong = array(root, "wrongQuestions");
        JsonNode words = array(root, "vocabulary");
        JsonNode scores = array(root, "scores");
        JsonNode masteredWords = array(root, "masteredWords");
        boolean hasTarget = root.hasNonNull("targetScore") && root.path("targetScore").isNumber();
        pendingImport = root;
        StringBuilder preview = new StringBuilder();
        String message = root.path("message").asText("").trim();
        if (!message.isBlank()) preview.append(message).append("\n\n");
        preview.append("AI 已自动分类：\n")
                .append("• 错题：").append(wrong.size()).append(" 道\n")
                .append("• 词汇：").append(words.size()).append(" 个\n")
                .append("• 模拟成绩：").append(scores.size()).append(" 条\n")
                .append("• 更新掌握状态：").append(masteredWords.size()).append(" 个\n")
                .append("• 目标分数：").append(hasTarget ? root.path("targetScore").asInt() + " 分" : "不修改").append("\n\n");
        int number = 1;
        for (JsonNode item : wrong) {
            preview.append("错题 ").append(number++).append("｜").append(item.path("type").asText("阅读")).append("\n")
                    .append(item.path("content").asText()).append("\n答案：")
                    .append(item.path("answer").asText()).append("\n\n");
        }
        number = 1;
        for (JsonNode item : words) {
            preview.append("词汇 ").append(number++).append("｜").append(item.path("word").asText())
                    .append("：").append(item.path("meaning").asText()).append("\n");
        }
        if (!words.isEmpty()) preview.append('\n');
        for (JsonNode item : scores) {
            preview.append("成绩｜").append(item.path("date").asText()).append("  听力 ")
                    .append(item.path("listening").asDouble()).append("  阅读 ")
                    .append(item.path("reading").asDouble()).append("  写作翻译 ")
                    .append(item.path("writing").asDouble()).append("\n");
        }
        if (!masteredWords.isEmpty()) {
            preview.append("\n标记为已掌握：");
            for (JsonNode word : masteredWords) preview.append(word.asText()).append(' ');
            preview.append('\n');
        }
        int total = wrong.size() + words.size() + scores.size() + masteredWords.size() + (hasTarget ? 1 : 0);
        if (total == 0) preview.append("没有识别出可以写入数据库的完整记录，请补充资料后重试。");
        output.setText(preview.toString());
        output.setCaretPosition(0);
        saveButton.setEnabled(total > 0);
    }

    private JsonNode array(JsonNode root, String name) {
        JsonNode value = root.path(name);
        if (!value.isArray()) throw new IllegalArgumentException("模型结果缺少 " + name + " 数组，请重试或更换模型。");
        return value;
    }

    private String extractJson(String response) {
        String value = response.trim();
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start < 0 || end <= start) throw new IllegalArgumentException("模型没有返回可识别的 JSON 数据。");
        return value.substring(start, end + 1);
    }

    private void saveAllData() {
        if (pendingImport == null) return;
        int choice = JOptionPane.showConfirmDialog(this,
                "确认把 AI 预览中的错题、词汇和成绩一次性写入本地数据库吗？",
                "确认智能导入", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;
        try {
            SaveCounts counts = saveTransaction(pendingImport);
            JOptionPane.showMessageDialog(this,
                    "处理完成：错题 " + counts.wrong() + " 道，词汇 " + counts.words() + " 个，成绩 " + counts.scores()
                            + " 条，掌握状态更新 " + counts.mastered() + " 个"
                            + (counts.targetChanged() ? "，目标分数已更新。" : "。"),
                    "AI 已完成整理", JOptionPane.INFORMATION_MESSAGE);
            saveButton.setEnabled(false);
            runningStatus.setText("已写入数据库");
            refreshOverview();
            dataChanged.run();
        } catch (Exception exception) {
            showError(exception);
        }
    }

    private SaveCounts saveTransaction(JsonNode root) throws Exception {
        int wrongCount = 0;
        int wordCount = 0;
        int scoreCount = 0;
        int masteredCount = 0;
        boolean targetChanged = false;
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement wrong = connection.prepareStatement(
                         "INSERT INTO wrong_question(type,content,answer,note) VALUES(?,?,?,?)");
                 PreparedStatement word = connection.prepareStatement("""
                         INSERT INTO vocabulary(word,meaning,status) VALUES(?,?,?)
                         ON CONFLICT(word) DO UPDATE SET meaning=excluded.meaning, status=excluded.status
                         """);
                 PreparedStatement score = connection.prepareStatement(
                         "INSERT INTO exam_score(exam_date,listening,reading,writing,total) VALUES(?,?,?,?,?)");
                 PreparedStatement mastered = connection.prepareStatement(
                         "UPDATE vocabulary SET status='已掌握' WHERE word=?");
                 PreparedStatement target = connection.prepareStatement(
                         "UPDATE user_setting SET target_score=? WHERE id=1")) {
                for (JsonNode item : array(root, "wrongQuestions")) {
                    String content = item.path("content").asText().trim();
                    String answer = item.path("answer").asText().trim();
                    if (content.isBlank() || answer.isBlank()) continue;
                    String type = item.path("type").asText("阅读");
                    if (!List.of("听力", "阅读", "写作", "翻译").contains(type)) type = "阅读";
                    wrong.setString(1, type);
                    wrong.setString(2, content);
                    wrong.setString(3, answer);
                    wrong.setString(4, item.path("note").asText(""));
                    wrong.addBatch();
                    wrongCount++;
                }
                for (JsonNode item : array(root, "vocabulary")) {
                    String spelling = item.path("word").asText().trim();
                    String meaning = item.path("meaning").asText().trim();
                    if (spelling.isBlank() || meaning.isBlank()) continue;
                    word.setString(1, spelling);
                    word.setString(2, meaning);
                    word.setString(3, "已掌握".equals(item.path("status").asText()) ? "已掌握" : "未掌握");
                    word.addBatch();
                    wordCount++;
                }
                for (JsonNode item : array(root, "scores")) {
                    String date = item.path("date").asText().trim();
                    LocalDate.parse(date);
                    double listening = checkedScore(item, "listening", 248.5);
                    double reading = checkedScore(item, "reading", 248.5);
                    double writing = checkedScore(item, "writing", 213.0);
                    score.setString(1, date);
                    score.setDouble(2, listening);
                    score.setDouble(3, reading);
                    score.setDouble(4, writing);
                    score.setDouble(5, listening + reading + writing);
                    score.addBatch();
                    scoreCount++;
                }
                for (JsonNode item : array(root, "masteredWords")) {
                    String spelling = item.asText().trim();
                    if (spelling.isBlank()) continue;
                    mastered.setString(1, spelling);
                    mastered.addBatch();
                }
                if (root.hasNonNull("targetScore") && root.path("targetScore").isNumber()) {
                    int targetValue = root.path("targetScore").asInt();
                    if (targetValue < 0 || targetValue > 710) {
                        throw new IllegalArgumentException("目标分数必须在 0 到 710 之间。");
                    }
                    target.setInt(1, targetValue);
                    target.executeUpdate();
                    targetChanged = true;
                }
                wrong.executeBatch();
                word.executeBatch();
                score.executeBatch();
                for (int result : mastered.executeBatch()) {
                    if (result > 0) masteredCount += result;
                }
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
        return new SaveCounts(wrongCount, wordCount, scoreCount, masteredCount, targetChanged);
    }

    private double checkedScore(JsonNode item, String name, double maximum) {
        if (!item.has(name) || !item.path(name).isNumber()) throw new IllegalArgumentException("成绩记录缺少数字字段：" + name);
        double value = item.path(name).asDouble();
        if (value < 0 || value > maximum) throw new IllegalArgumentException("成绩字段 " + name + " 超出范围。");
        return value;
    }

    private void setBusy(boolean busy) {
        runButton.setEnabled(!busy);
        task.setEnabled(!busy);
        runningStatus.setText(busy ? "模型处理中……" : runningStatus.getText());
    }

    private String formatScore(double value) {
        return value == Math.rint(value) ? String.format("%.0f", value) : String.format("%.1f", value);
    }

    private void showError(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) message = exception.toString();
        JOptionPane.showMessageDialog(this, message, "AI 操作失败", JOptionPane.ERROR_MESSAGE);
    }

    private record Prompt(String system, String user) {}
    private record SaveCounts(int wrong, int words, int scores, int mastered, boolean targetChanged) {}
}
