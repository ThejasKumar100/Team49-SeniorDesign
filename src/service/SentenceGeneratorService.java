package service;

import dao.GeneratedSentenceDAO;
import dao.WordDAO;
import dao.WordFollowerDAO;
import model.Word;

import java.sql.SQLException;
import java.util.List;
import java.util.Random;

public class SentenceGeneratorService {
    private final WordFollowerDAO followerDAO;
    private final WordDAO wordDAO;
    private final GeneratedSentenceDAO generatedSentenceDAO;
    private final Random random;

    public SentenceGeneratorService() {
        this.followerDAO = new WordFollowerDAO();
        this.wordDAO = new WordDAO();
        this.generatedSentenceDAO = new GeneratedSentenceDAO();
        this.random = new Random();
    }



    public String generateSentenceMostFrequent(String startWord, int maxWords) throws SQLException {
        String sentence = generate(startWord, maxWords, 1);
        generatedSentenceDAO.insertGeneratedSentence(sentence);
        return sentence;
    }

    public String generateSentenceRandomTop3(String startWord, int maxWords) throws SQLException {
        String sentence = generate(startWord, maxWords, 2);
        generatedSentenceDAO.insertGeneratedSentence(sentence);
        return sentence;
    }

    public String generateSentenceRandomStartMostFrequent(int maxWords) throws SQLException {
        List<Word> starters = wordDAO.getSentenceStartWords();
        if (starters.isEmpty()) {
            throw new IllegalStateException("No sentence-start words found in database.");
        }

        Word start = starters.get(random.nextInt(Math.min(starters.size(), 20)));
        String sentence = generate(start.getWordText(), maxWords, 1);
        generatedSentenceDAO.insertGeneratedSentence(sentence);
        return sentence;
    }

    private String generate(String startWord, int maxWords, int algorithm) throws SQLException {
        String current = normalizeWord(startWord);
        if (current.isEmpty()) {
            throw new IllegalArgumentException("Start word cannot be empty.");
        }

        StringBuilder sentence = new StringBuilder(capitalize(current));

        for (int i = 1; i < maxWords; i++) {
            List<String> nextWords = followerDAO.getTopNextWords(current, 3);

            if (nextWords.isEmpty()) {
                break;
            }

            String next;
            if (algorithm == 1) {
                next = nextWords.get(0);
            } else {
                next = nextWords.get(random.nextInt(nextWords.size()));
            }

            sentence.append(" ").append(next);
            current = next;

            if (shouldEndSentence(current, i, maxWords)) {
                break;
            }
        }

        String finalSentence = sentence.toString().trim();
        if (!finalSentence.endsWith(".")) {
            finalSentence += ".";
        }
        return finalSentence;
    }

    private boolean shouldEndSentence(String currentWord, int currentLength, int maxWords) throws SQLException {
        if (currentLength < 4) {
            return false;
        }

        if (currentLength >= maxWords - 1) {
            return true;
        }

        Word word = wordDAO.findByText(currentWord).orElse(null);
        if (word == null) {
            return false;
        }

        if (word.isCanEnd() && currentLength >= 7) {
            return random.nextDouble() < 0.35;
        }

        return false;
    }

    private String normalizeWord(String word) {
        return word == null ? "" : word.toLowerCase().trim().replaceAll("^[^a-zA-Z']+|[^a-zA-Z']+$", "");
    }

    private String capitalize(String word) {
        if (word == null || word.isEmpty()) {
            return word;
        }
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }
}