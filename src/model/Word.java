package model;

public class Word {
    private int wordId;
    private String wordText;
    private int totalOccurrences;
    private int startCount;
    private int endCount;
    private boolean canStart;
    private boolean canEnd;

    public Word(int wordId, String wordText, int totalOccurrences,
                int startCount, int endCount, boolean canStart, boolean canEnd) {
        this.wordId = wordId;
        this.wordText = wordText;
        this.totalOccurrences = totalOccurrences;
        this.startCount = startCount;
        this.endCount = endCount;
        this.canStart = canStart;
        this.canEnd = canEnd;
    }

    public int getWordId() {
        return wordId;
    }

    public String getWordText() {
        return wordText;
    }

    public int getTotalOccurrences() {
        return totalOccurrences;
    }

    public int getStartCount() {
        return startCount;
    }

    public int getEndCount() {
        return endCount;
    }

    public boolean isCanStart() {
        return canStart;
    }

    public boolean isCanEnd() {
        return canEnd;
    }

    @Override
    public String toString() {
        return "Word{" +
                "wordId=" + wordId +
                ", wordText='" + wordText + '\'' +
                ", totalOccurrences=" + totalOccurrences +
                ", startCount=" + startCount +
                ", endCount=" + endCount +
                ", canStart=" + canStart +
                ", canEnd=" + canEnd +
                '}';
    }
}