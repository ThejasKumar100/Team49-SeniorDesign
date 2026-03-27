package dao;

import model.GeneratedSentence;
import util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GeneratedSentenceDAO {

    public void insertGeneratedSentence(String sentenceText) throws SQLException {
        String sql = "INSERT INTO Generated_Sentences (sentence_text) VALUES (?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sentenceText);
            stmt.executeUpdate();
        }
    }

    public List<GeneratedSentence> getAllGeneratedSentences() throws SQLException {
        List<GeneratedSentence> sentences = new ArrayList<>();
        String sql = "SELECT * FROM Generated_Sentences ORDER BY generated_at DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("generated_at");
                sentences.add(new GeneratedSentence(
                        rs.getInt("sentence_id"),
                        rs.getString("sentence_text"),
                        ts == null ? null : ts.toLocalDateTime()
                ));
            }
        }

        return sentences;
    }

    public List<GeneratedSentence> getDuplicateSentences() throws SQLException {
        List<GeneratedSentence> duplicates = new ArrayList<>();
        String sql = "SELECT gs.* " +
                "FROM Generated_Sentences gs " +
                "JOIN (" +
                "  SELECT sentence_text " +
                "  FROM Generated_Sentences " +
                "  GROUP BY sentence_text " +
                "  HAVING COUNT(*) > 1" +
                ") d ON gs.sentence_text = d.sentence_text " +
                "ORDER BY gs.sentence_text, gs.generated_at DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("generated_at");
                duplicates.add(new GeneratedSentence(
                        rs.getInt("sentence_id"),
                        rs.getString("sentence_text"),
                        ts == null ? null : ts.toLocalDateTime()
                ));
            }
        }

        return duplicates;
    }
}