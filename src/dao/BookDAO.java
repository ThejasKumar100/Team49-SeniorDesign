package dao;

import model.Book;
import util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for the Books table.
 *
 * Responsibilities:
 * - Insert new book records into the database
 * - Retrieve stored book metadata for reporting/display
 *
 * This class abstracts all SQL operations related to books,
 * keeping database logic separate from business logic.
 */
public class BookDAO {

    /**
     * Inserts a new book record into the database.
     *
     * @param fileName  name of the imported file
     * @param wordCount total number of words in the file
     * @return generated primary key (book_id) for the inserted record
     * @throws SQLException if insertion fails
     */
    public int insertBook(String fileName, int wordCount) throws SQLException {

        // SQL query uses placeholders (?) to prevent SQL injection
        String sql = "INSERT INTO Books (file_name, word_count) VALUES (?, ?)";

        // try-with-resources ensures connection and statement are auto-closed
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // Bind parameters to SQL query
            stmt.setString(1, fileName);
            stmt.setInt(2, wordCount);

            // Execute insert operation
            stmt.executeUpdate();

            // Retrieve auto-generated primary key (book_id)
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        // If no key is returned, insertion failed unexpectedly
        throw new SQLException("Could not insert book.");
    }

    /**
     * Retrieves all books from the database.
     *
     * Books are ordered by most recently imported first.
     *
     * @return list of Book objects representing database rows
     * @throws SQLException if query fails
     */
    public List<Book> getAllBooks() throws SQLException {

        List<Book> books = new ArrayList<>();

        // Order by imported_at DESC → newest files appear first in UI
        String sql = "SELECT * FROM Books ORDER BY imported_at DESC";

        // Execute query and iterate through results
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {

                // Retrieve timestamp and safely convert to LocalDateTime
                Timestamp ts = rs.getTimestamp("imported_at");

                // Map database row → Book model object
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