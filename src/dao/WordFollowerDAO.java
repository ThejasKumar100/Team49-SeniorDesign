package dao;

import model.WordFollower;
import util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WordFollowerDAO {

    public void insertOrUpdateFollower(int wordId, int nextWordId) throws SQLException {
        String checkSql = "SELECT relation_id FROM Word_Followers WHERE word_id = ? AND next_word_id = ?";
        String updateSql = "UPDATE Word_Followers SET follow_count = follow_count + 1 WHERE relation_id = ?";
        String insertSql = "INSERT INTO Word_Followers (word_id, next_word_id, follow_count) VALUES (?, ?, 1)";

        try (Connection conn = DatabaseManager.getConnection()) {
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, wordId);
                checkStmt.setInt(2, nextWordId);

                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        int relationId = rs.getInt("relation_id");
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setInt(1, relationId);
                            updateStmt.executeUpdate();
                        }
                    } else {
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