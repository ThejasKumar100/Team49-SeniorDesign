package model;

public class WordFollower {
    private int relationId;
    private int wordId;
    private int nextWordId;
    private int followCount;

    public WordFollower(int relationId, int wordId, int nextWordId, int followCount) {
        this.relationId = relationId;
        this.wordId = wordId;
        this.nextWordId = nextWordId;
        this.followCount = followCount;
    }

    public int getRelationId() {
        return relationId;
    }

    public int getWordId() {
        return wordId;
    }

    public int getNextWordId() {
        return nextWordId;
    }

    public int getFollowCount() {
        return followCount;
    }

    @Override
    public String toString() {
        return "WordFollower{" +
                "relationId=" + relationId +
                ", wordId=" + wordId +
                ", nextWordId=" + nextWordId +
                ", followCount=" + followCount +
                '}';
    }
}