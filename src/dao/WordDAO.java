package dao;

import model.Word;
import util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WordDAO {

    public Optional<Word> findByText(String wordText) throws SQLException {
        String sql = "SELECT * FROM Words WHERE word_text = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, wordText);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }

        return Optional.empty();
    }

    public Optional<Word> findById(int wordId) throws SQLException {
        String sql = "SELECT * FROM Words WHERE word_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, wordId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }

        return Optional.empty();
    }

    public int insertWord(String wordText, boolean isSentenceStart, boolean isSentenceEnd) throws SQLException {
        String sql = "INSERT INTO Words (word_text, total_occurrences, start_count, end_count, can_start, can_end) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, wordText);
            stmt.setInt(2, 1);
            stmt.setInt(3, isSentenceStart ? 1 : 0);
            stmt.setInt(4, isSentenceEnd ? 1 : 0);
            stmt.setBoolean(5, isSentenceStart);
            stmt.setBoolean(6, isSentenceEnd);
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        throw new SQLException("Could not insert word.");
    }

    public void updateWordCounts(int wordId, boolean isSentenceStart, boolean isSentenceEnd) throws SQLException {
        String sql = "UPDATE Words " +
                "SET total_occurrences = total_occurrences + 1, " +
                "start_count = start_count + ?, " +
                "end_count = end_count + ?, " +
                "can_start = CASE WHEN ? THEN TRUE ELSE can_start END, " +
                "can_end = CASE WHEN ? THEN TRUE ELSE can_end END " +
                "WHERE word_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, isSentenceStart ? 1 : 0);
            stmt.setInt(2, isSentenceEnd ? 1 : 0);
            stmt.setBoolean(3, isSentenceStart);
            stmt.setBoolean(4, isSentenceEnd);
            stmt.setInt(5, wordId);
            stmt.executeUpdate();
        }
    }

    public int getOrCreateWord(String wordText, boolean isSentenceStart, boolean isSentenceEnd) throws SQLException {
        Optional<Word> existing = findByText(wordText);
        if (existing.isPresent()) {
            updateWordCounts(existing.get().getWordId(), isSentenceStart, isSentenceEnd);
            return existing.get().getWordId();
        }
        return insertWord(wordText, isSentenceStart, isSentenceEnd);
    }

    public void insertUnknownWordIfMissing(String wordText) throws SQLException {
        Optional<Word> existing = findByText(wordText);
        if (!existing.isPresent()) {
            String sql = "INSERT INTO Words (word_text, total_occurrences, start_count, end_count, can_start, can_end) VALUES (?, 0, 0, 0, FALSE, FALSE)";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, wordText);
                stmt.executeUpdate();
            }
        }
    }

    public void updateWordMetadata(int wordId, int totalOccurrences, int startCount, int endCount,
                                   boolean canStart, boolean canEnd) throws SQLException {
        String sql = "UPDATE Words SET total_occurrences = ?, start_count = ?, end_count = ?, can_start = ?, can_end = ? WHERE word_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, totalOccurrences);
            stmt.setInt(2, startCount);
            stmt.setInt(3, endCount);
            stmt.setBoolean(4, canStart);
            stmt.setBoolean(5, canEnd);
            stmt.setInt(6, wordId);
            stmt.executeUpdate();
        }
    }

    public List<Word> getAllWordsAlphabetical() throws SQLException {
        List<Word> words = new ArrayList<>();
        String sql = "SELECT * FROM Words ORDER BY word_text ASC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                words.add(mapRow(rs));
            }
        }

        return words;
    }

    public List<Word> getAllWordsByFrequency() throws SQLException {
        List<Word> words = new ArrayList<>();
        String sql = "SELECT * FROM Words ORDER BY total_occurrences DESC, word_text ASC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                words.add(mapRow(rs));
            }
        }

        return words;
    }

    public List<Word> getSentenceStartWords() throws SQLException {
        List<Word> words = new ArrayList<>();
        String sql = "SELECT * FROM Words WHERE can_start = TRUE ORDER BY start_count DESC, word_text ASC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                words.add(mapRow(rs));
            }
        }

        return words;
    }

    private Word mapRow(ResultSet rs) throws SQLException {
        return new Word(
                rs.getInt("word_id"),
                rs.getString("word_text"),
                rs.getInt("total_occurrences"),
                rs.getInt("start_count"),
                rs.getInt("end_count"),
                rs.getBoolean("can_start"),
                rs.getBoolean("can_end")
        );
    }
}