package dao;

import model.Book;
import util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookDAO {

    public int insertBook(String fileName, int wordCount) throws SQLException {
        String sql = "INSERT INTO Books (file_name, word_count) VALUES (?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, fileName);
            stmt.setInt(2, wordCount);
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        throw new SQLException("Could not insert book.");
    }

    public List<Book> getAllBooks() throws SQLException {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM Books ORDER BY imported_at DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("imported_at");
                books.add(new Book(
                        rs.getInt("book_id"),
                        rs.getString("file_name"),
                        rs.getInt("word_count"),
                        ts == null ? null : ts.toLocalDateTime()
                ));
            }
        }

        return books;
    }
}