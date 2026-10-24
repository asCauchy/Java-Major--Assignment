package cn.edu.cetsimple;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Database {
    private static final String URL = "jdbc:sqlite:data/cet-simple.db";

    public static Connection getConnection() throws Exception {
        Files.createDirectories(Path.of("data"));
        return DriverManager.getConnection(URL);
    }

    public static void createTables() throws Exception {
        String wrongQuestionTable = """
                CREATE TABLE IF NOT EXISTS wrong_question (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    type TEXT NOT NULL,
                    content TEXT NOT NULL,
                    answer TEXT NOT NULL,
                    note TEXT
                )
                """;

        String vocabularyTable = """
                CREATE TABLE IF NOT EXISTS vocabulary (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    word TEXT NOT NULL UNIQUE,
                    meaning TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT '未掌握'
                )
                """;

        String scoreTable = """
                CREATE TABLE IF NOT EXISTS exam_score (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    exam_date TEXT NOT NULL,
                    listening REAL NOT NULL,
                    reading REAL NOT NULL,
                    writing REAL NOT NULL,
                    total REAL NOT NULL
                )
                """;

        String settingTable = """
                CREATE TABLE IF NOT EXISTS user_setting (
                    id INTEGER PRIMARY KEY,
                    target_score INTEGER NOT NULL DEFAULT 500
                )
                """;

        String reviewLogTable = """
                CREATE TABLE IF NOT EXISTS review_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    item_type TEXT NOT NULL,
                    item_id INTEGER NOT NULL,
                    result TEXT NOT NULL,
                    reviewed_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """;

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(wrongQuestionTable);
            statement.execute(vocabularyTable);
            statement.execute(scoreTable);
            statement.execute(settingTable);
            statement.execute(reviewLogTable);
            statement.execute("INSERT OR IGNORE INTO user_setting(id, target_score) VALUES(1, 500)");
        }
        ensureColumn("wrong_question", "familiarity", "INTEGER NOT NULL DEFAULT 0");
        ensureColumn("wrong_question", "last_review", "TEXT");
        ensureColumn("wrong_question", "next_review", "TEXT");
        ensureColumn("wrong_question", "correct_streak", "INTEGER NOT NULL DEFAULT 0");
        ensureColumn("vocabulary", "familiarity", "INTEGER NOT NULL DEFAULT 0");
        ensureColumn("vocabulary", "last_review", "TEXT");
        ensureColumn("vocabulary", "next_review", "TEXT");
        ensureColumn("vocabulary", "correct_streak", "INTEGER NOT NULL DEFAULT 0");
    }

    public static int getTargetScore() throws Exception {
        String sql = "SELECT target_score FROM user_setting WHERE id=1";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            return result.next() ? result.getInt("target_score") : 500;
        }
    }

    public static void saveTargetScore(int score) throws Exception {
        String sql = "UPDATE user_setting SET target_score=? WHERE id=1";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, score);
            statement.executeUpdate();
        }
    }

    public static DashboardStats loadDashboardStats() throws Exception {
        String sql = """
                SELECT
                  (SELECT COUNT(*) FROM wrong_question) AS wrong_count,
                  (SELECT COUNT(*) FROM vocabulary) AS word_count,
                  (SELECT COUNT(*) FROM vocabulary WHERE status='已掌握') AS mastered_count,
                  COALESCE((SELECT total FROM exam_score ORDER BY exam_date DESC, id DESC LIMIT 1), 0) AS latest_score
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            return new DashboardStats(
                    result.getInt("wrong_count"),
                    result.getInt("word_count"),
                    result.getInt("mastered_count"),
                    result.getDouble("latest_score")
            );
        }
    }

    public static List<ReviewItem> loadDueReviewItems(int limit) throws Exception {
        List<ReviewItem> items = new ArrayList<>();
        String today = LocalDate.now().toString();
        String vocabularySql = """
                SELECT id, word, meaning, familiarity
                FROM vocabulary
                WHERE next_review IS NULL OR next_review <= ?
                ORDER BY CASE WHEN next_review IS NULL THEN 0 ELSE 1 END, next_review, id
                LIMIT ?
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(vocabularySql)) {
            statement.setString(1, today);
            statement.setInt(2, Math.max(1, limit / 2));
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    items.add(new ReviewItem("vocabulary", result.getInt("id"),
                            result.getString("word"), result.getString("meaning"),
                            "词汇", result.getInt("familiarity")));
                }
            }
        }
        String wrongSql = """
                SELECT id, type, content, answer, note, familiarity
                FROM wrong_question
                WHERE next_review IS NULL OR next_review <= ?
                ORDER BY CASE WHEN next_review IS NULL THEN 0 ELSE 1 END, next_review, id
                LIMIT ?
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(wrongSql)) {
            statement.setString(1, today);
            statement.setInt(2, Math.max(1, limit - items.size()));
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    String answer = result.getString("answer");
                    String note = result.getString("note");
                    String back = answer + (note == null || note.isBlank() ? "" : "\n\n解析：" + note);
                    items.add(new ReviewItem("wrong_question", result.getInt("id"),
                            result.getString("content"), back,
                            result.getString("type") + "错题", result.getInt("familiarity")));
                }
            }
        }
        return items.size() <= limit ? items : new ArrayList<>(items.subList(0, limit));
    }

    public static void recordReview(ReviewItem item, String result) throws Exception {
        int familiarity = item.familiarity();
        int nextFamiliarity;
        int interval;
        boolean correct = "GOOD".equals(result);
        if ("AGAIN".equals(result)) {
            nextFamiliarity = Math.max(0, familiarity - 1);
            interval = 1;
        } else if ("HARD".equals(result)) {
            nextFamiliarity = familiarity;
            interval = 2;
        } else {
            nextFamiliarity = Math.min(5, familiarity + 1);
            int[] intervals = {1, 3, 7, 14, 30, 60};
            interval = intervals[nextFamiliarity];
        }
        String table = "vocabulary".equals(item.itemType()) ? "vocabulary" : "wrong_question";
        String update = "UPDATE " + table + " SET familiarity=?, last_review=?, next_review=?, "
                + "correct_streak=CASE WHEN ? THEN correct_streak+1 ELSE 0 END WHERE id=?";
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(update);
                 PreparedStatement log = connection.prepareStatement(
                         "INSERT INTO review_log(item_type,item_id,result,reviewed_at) VALUES(?,?,?,CURRENT_TIMESTAMP)")) {
                statement.setInt(1, nextFamiliarity);
                statement.setString(2, LocalDate.now().toString());
                statement.setString(3, LocalDate.now().plusDays(interval).toString());
                statement.setBoolean(4, correct);
                statement.setInt(5, item.id());
                statement.executeUpdate();
                if ("vocabulary".equals(table) && nextFamiliarity >= 3) {
                    try (PreparedStatement mastered = connection.prepareStatement(
                            "UPDATE vocabulary SET status='已掌握' WHERE id=?")) {
                        mastered.setInt(1, item.id());
                        mastered.executeUpdate();
                    }
                }
                log.setString(1, item.itemType());
                log.setInt(2, item.id());
                log.setString(3, result);
                log.executeUpdate();
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public static LearningProgress loadLearningProgress() throws Exception {
        String today = LocalDate.now().toString();
        int dueWords = scalarInt("SELECT COUNT(*) FROM vocabulary WHERE next_review IS NULL OR next_review <= ?", today);
        int dueWrong = scalarInt("SELECT COUNT(*) FROM wrong_question WHERE next_review IS NULL OR next_review <= ?", today);
        int reviewedToday = scalarInt("SELECT COUNT(*) FROM review_log WHERE date(reviewed_at)=?", today);
        int reviewedWeek = scalarInt("SELECT COUNT(*) FROM review_log WHERE date(reviewed_at)>=?",
                LocalDate.now().minusDays(6).toString());
        int goodWeek = scalarInt("SELECT COUNT(*) FROM review_log WHERE result='GOOD' AND date(reviewed_at)>=?",
                LocalDate.now().minusDays(6).toString());
        int streak = calculateStreak();
        int accuracy = reviewedWeek == 0 ? 0 : (int) Math.round(goodWeek * 100.0 / reviewedWeek);
        return new LearningProgress(dueWords, dueWrong, reviewedToday, reviewedWeek, accuracy, streak);
    }

    private static int calculateStreak() throws Exception {
        List<LocalDate> days = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT DISTINCT date(reviewed_at) AS day FROM review_log ORDER BY day DESC");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) days.add(LocalDate.parse(result.getString("day")));
        }
        if (days.isEmpty()) return 0;
        LocalDate expected = LocalDate.now();
        if (!days.get(0).equals(expected)) {
            expected = expected.minusDays(1);
            if (!days.get(0).equals(expected)) return 0;
        }
        int streak = 0;
        for (LocalDate day : days) {
            if (!day.equals(expected)) break;
            streak++;
            expected = expected.minusDays(1);
        }
        return streak;
    }

    private static int scalarInt(String sql, String value) throws Exception {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        }
    }

    private static void ensureColumn(String table, String column, String definition) throws Exception {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("PRAGMA table_info(" + table + ")");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                if (column.equalsIgnoreCase(result.getString("name"))) return;
            }
        }
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    public record DashboardStats(int wrongCount, int wordCount, int masteredCount, double latestScore) {}
    public record ReviewItem(String itemType, int id, String front, String back, String category, int familiarity) {}
    public record LearningProgress(int dueWords, int dueWrong, int reviewedToday,
                                   int reviewedWeek, int accuracyWeek, int streakDays) {}
}
