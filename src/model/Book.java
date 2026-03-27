package model;

import java.time.LocalDateTime;

public class Book {
    private int bookId;
    private String fileName;
    private int wordCount;
    private LocalDateTime importedAt;

    public Book(int bookId, String fileName, int wordCount, LocalDateTime importedAt) {
        this.bookId = bookId;
        this.fileName = fileName;
        this.wordCount = wordCount;
        this.importedAt = importedAt;
    }

    public int getBookId() {
        return bookId;
    }

    public String getFileName() {
        return fileName;
    }

    public int getWordCount() {
        return wordCount;
    }

    public LocalDateTime getImportedAt() {
        return importedAt;
    }

    @Override
    public String toString() {
        return "Book{" +
                "bookId=" + bookId +
                ", fileName='" + fileName + '\'' +
                ", wordCount=" + wordCount +
                ", importedAt=" + importedAt +
                '}';
    }
}