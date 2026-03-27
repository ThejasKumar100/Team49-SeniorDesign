package service;

import dao.BookDAO;
import dao.WordDAO;
import dao.WordFollowerDAO;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TextImportService {
    private final BookDAO bookDAO;
    private final WordDAO wordDAO;
    private final WordFollowerDAO wordFollowerDAO;

    public TextImportService() {
        this.bookDAO = new BookDAO();
        this.wordDAO = new WordDAO();
        this.wordFollowerDAO = new WordFollowerDAO();
    }

    public void importTextFile(String filePath) throws IOException, SQLException {
        Path path = Path.of(filePath);
        String content = Files.readString(path, StandardCharsets.UTF_8);

        List<List<String>> sentences = splitIntoSentences(content);
        int totalWords = countTotalWords(sentences);

        bookDAO.insertBook(path.getFileName().toString(), totalWords);

        for (List<String> sentence : sentences) {
            if (sentence.isEmpty()) {
                continue;
            }

            for (int i = 0; i < sentence.size(); i++) {
                String currentWord = sentence.get(i);
                boolean isSentenceStart = (i == 0);
                boolean isSentenceEnd = (i == sentence.size() - 1);

                int currentWordId = wordDAO.getOrCreateWord(currentWord, isSentenceStart, isSentenceEnd);

                if (!isSentenceEnd) {
                    String nextWord = sentence.get(i + 1);
                    int nextWordId = wordDAO.getOrCreateWord(nextWord, false, false);
                    wordFollowerDAO.insertOrUpdateFollower(currentWordId, nextWordId);
                }
            }
        }
    }

    private int countTotalWords(List<List<String>> sentences) {
        int count = 0;
        for (List<String> sentence : sentences) {
            count += sentence.size();
        }
        return count;
    }

    private List<List<String>> splitIntoSentences(String text) {
        List<List<String>> result = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return result;
        }

        String[] rawSentences = text.split("[.!?]+");

        for (String rawSentence : rawSentences) {
            List<String> tokens = tokenize(rawSentence);
            if (!tokens.isEmpty()) {
                result.add(tokens);
            }
        }

        return result;
    }

    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return tokens;
        }

        String cleaned = text.toLowerCase().replaceAll("[^a-zA-Z'\\s-]", " ");
        String[] rawWords = cleaned.split("\\s+");

        for (String rawWord : rawWords) {
            String word = normalizeWord(rawWord);
            if (!word.isEmpty()) {
                tokens.add(word);
            }
        }

        return tokens;
    }

    private String normalizeWord(String rawWord) {
        if (rawWord == null) {
            return "";
        }

        String word = rawWord.trim();
        word = word.replaceAll("^[^a-zA-Z']+|[^a-zA-Z']+$", "");
        return word.toLowerCase();
    }
}