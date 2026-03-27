package service;

import dao.WordDAO;
import dao.WordFollowerDAO;

import java.sql.SQLException;
import java.util.List;

public class AutocompleteService {
    private final WordFollowerDAO wordFollowerDAO;
    private final WordDAO wordDAO;

    public AutocompleteService() {
        this.wordFollowerDAO = new WordFollowerDAO();
        this.wordDAO = new WordDAO();
    }

    public List<String> suggestNextWords(String completedWord) throws SQLException {
        String normalized = normalizeWord(completedWord);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Completed word cannot be empty.");
        }
        return wordFollowerDAO.getTopNextWords(normalized, 5);
    }

    public void addUnknownWordIfMissing(String typedWord) throws SQLException {
        String normalized = normalizeWord(typedWord);
        if (!normalized.isEmpty()) {
            wordDAO.insertUnknownWordIfMissing(normalized);
        }
    }

    private String normalizeWord(String word) {
        return word == null ? "" : word.toLowerCase().trim().replaceAll("^[^a-zA-Z']+|[^a-zA-Z']+$", "");
    }
}