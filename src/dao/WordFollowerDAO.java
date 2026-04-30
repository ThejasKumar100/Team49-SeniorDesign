package dao;

import model.WordFollower;
import util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for the Word_Followers table.
 *
 * This class manages relationships between words, specifically tracking
 * which words follow others and how frequently they occur together.
 *
 * Core purpose:
 * - Build a probabilistic model of word sequences
 * - Enable sentence generation and autocomplete features
 *
 * Key responsibilities:
 * - Insert or update word-to-word relationships
 * - Retrieve followers of a given word
 * - Provide ranked next-word suggestions based on frequency
 */
public class WordFollowerDAO {

    /**
     * Inserts a new word relationship or updates an existing one.
     *
     * If the (wordId → nextWordId) relationship already exists,
     * it increments the follow_count.
     * Otherwise, it inserts a new relationship with count = 1.
     *
     * This is the core method used during text ingestion to build
     * word transition probabilities.
     */
    public void insertOrUpdateFollower(int wordId, int nextWordId) throws SQLException {
        String checkSql = "SELECT relation_id FROM Word_Followers WHERE word_id = ? AND next_word_id = ?";
        String updateSql = "UPDATE Word_Followers SET follow_count = follow_count + 1 WHERE relation_id = ?";
        String insertSql = "INSERT INTO Word_Followers (word_id, next_word_id, follow_count) VALUES (?, ?, 1)";

        try (Connection conn = DatabaseManager.getConnection()) {

            // Check if relationship already exists
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, wordId);
                checkStmt.setInt(2, nextWordId);

                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        // Relationship exists → increment follow count
                        int relationId = rs.getInt("relation_id");

                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setInt(1, relationId);
                            updateStmt.executeUpdate();
                        }
                    } else {
                        // Relationship does not exist → insert new record
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                            insertStmt.setInt(1, wordId);
                            insertStmt.setInt(2, nextWordId);
                            insertStmt.executeUpdate();
                        }
                    }
                }
            }
        }
    }

    /**
     * Retrieves all follower relationships for a given word ID.
     *
     * Results are sorted by follow_count in descending order,
     * meaning the most likely next words appear first.
     */
    public List<WordFollower> getFollowersForWordId(int wordId) throws SQLException {
        List<WordFollower> followers = new ArrayList<>();
        String sql = "SELECT * FROM Word_Followers WHERE word_id = ? ORDER BY follow_count DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, wordId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    followers.add(new WordFollower(
                            rs.getInt("relation_id"),
                            rs.getInt("word_id"),
                            rs.getInt("next_word_id"),
                            rs.getInt("follow_count")
                    ));
                }
            }
        }

        return followers;
    }

    /**
     * Retrieves the top N most likely next words for a given word.
     *
     * Uses SQL joins to:
     * - Map the current word to its ID
     * - Find its follower relationships
     * - Retrieve the corresponding next word text
     *
     * Results are ranked by:
     * 1. Highest follow_count (most frequent transitions)
     * 2. Alphabetical order (tie-breaker)
     *
     * Used for:
     * - Sentence generation
     * - Autocomplete suggestions
     */
    public List<String> getTopNextWords(String currentWord, int limit) throws SQLException {
        List<String> nextWords = new ArrayList<>();

        String sql = "SELECT w2.word_text " +
                "FROM Words w1 " +
                "JOIN Word_Followers wf ON w1.word_id = wf.word_id " +
                "JOIN Words w2 ON wf.next_word_id = w2.word_id " +
                "WHERE w1.word_text = ? " +
                "ORDER BY wf.follow_count DESC, w2.word_text ASC " +
                "LIMIT ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, currentWord);
            stmt.setInt(2, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    nextWords.add(rs.getString("word_text"));
                }
            }
        }

        return nextWords;
    }

    /**
     * Retrieves ALL possible next words for a given word (no limit).
     *
     * Similar to getTopNextWords but returns the full list of
     * next-word candidates ranked by frequency.
     *
     * Useful for:
     * - Advanced generation logic
     * - Randomized selection strategies (e.g., top 3 sampling)
     */
    public List<String> getAllNextWords(String currentWord) throws SQLException {
        List<String> nextWords = new ArrayList<>();

        String sql = "SELECT w2.word_text " +
                "FROM Words w1 " +
                "JOIN Word_Followers wf ON w1.word_id = wf.word_id " +
                "JOIN Words w2 ON wf.next_word_id = w2.word_id " +
                "WHERE w1.word_text = ? " +
                "ORDER BY wf.follow_count DESC, w2.word_text ASC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, currentWord);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    nextWords.add(rs.getString("word_text"));
                }
            }
        }

        return nextWords;
    }
}