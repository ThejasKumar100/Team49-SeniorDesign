package model;

import java.time.LocalDateTime;

public class GeneratedSentence {
    private int sentenceId;
    private String sentenceText;
    private LocalDateTime generatedAt;

    public GeneratedSentence(int sentenceId, String sentenceText, LocalDateTime generatedAt) {
        this.sentenceId = sentenceId;
        this.sentenceText = sentenceText;
        this.generatedAt = generatedAt;
    }

    public int getSentenceId() {
        return sentenceId;
    }

    public String getSentenceText() {
        return sentenceText;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    @Override
    public String toString() {
        return "GeneratedSentence{" +
                "sentenceId=" + sentenceId +
                ", sentenceText='" + sentenceText + '\'' +
                ", generatedAt=" + generatedAt +
                '}';
    }
}