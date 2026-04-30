package dao;

import model.GeneratedSentence;
import util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for the Generated_Sentences table.
 *
 * Responsibilities:
 * - Insert generated sentences into the database
 * - Retrieve all generated sentences for reporting
 * - Identify duplicate sentences using SQL aggregation
 *
 * This class isolates all database operations related to sentence outputs.
 */
public class GeneratedSentenceDAO {

    /**
     * Inserts a newly generated sentence into the database.
     *
     * @param sentenceText the generated sentence string
     * @throws SQLException if the insert operation fails
     */
    public void insertGeneratedSentence(String sentenceText) throws SQLException {

        // Prepared statement prevents SQL injection
        String sql = "INSERT INTO Generated_Sentences (sentence_text) VALUES (?)";

        // try-with-resources ensures connection and statement are automatically closed
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sentenceText);
            stmt.executeUpdate();
        }
    }

    /**
     * Retrieves all generated sentences from the database.
     *
     * Results are ordered by most recently generated first.
     *
     * @return list of GeneratedSentence objects
     * @throws SQLException if query execution fails
     */
    public List<GeneratedSentence> getAllGeneratedSentences() throws SQLException {

        List<GeneratedSentence> sentences = new ArrayList<>();

        // Order by generated_at DESC → newest sentences appear first in UI
        String sql = "SELECT * FROM Generated_Sentences ORDER BY generated_at DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {

                // Safely convert SQL timestamp to Java LocalDateTime
                Timestamp ts = rs.getTimestamp("generated_at");

                // Map database row → GeneratedSentence object
                sentences.add(new GeneratedSentence(
                        rs.getInt("sentence_id"),
                        rs.getString("sentence_text"),
                        ts == null ? null : ts.toLocalDateTime()
                ));
            }
        }

        return sentences;
    }

    /**
     * Retrieves duplicate generated sentences.
     *
     * A sentence is considered a duplicate if it appears more than once
     * in the Generated_Sentences table.
     *
     * SQL Breakdown:
     * 1. Inner query groups sentences by text and finds those with COUNT > 1
     * 2. Outer query joins back to retrieve full rows for those duplicates
     *
     * @return list of duplicate GeneratedSentence objects
     * @throws SQLException if query fails
     */
    public List<GeneratedSentence> getDuplicateSentences() throws SQLException {

        List<GeneratedSentence> duplicates = new ArrayList<>();

        String sql =
                "SELECT gs.* " +
                "FROM Generated_Sentences gs " +
                "JOIN (" +
                "  SELECT sentence_text " +
                "  FROM Generated_Sentences " +
                "  GROUP BY sentence_text " +
                "  HAVING COUNT(*) > 1" +   // identifies duplicate sentence texts
                ") d ON gs.sentence_text = d.sentence_text " +
                "ORDER BY gs.sentence_text, gs.generated_at DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {

                // Convert timestamp safely
                Timestamp ts = rs.getTimestamp("generated_at");

                // Map row → object
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