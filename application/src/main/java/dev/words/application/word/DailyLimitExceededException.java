package dev.words.application.word;

/**
 * The user has already added as many new words today as allowed. Words already in the deck can still be opened.
 */
public class DailyLimitExceededException extends RuntimeException {

    private final int limit;

    public DailyLimitExceededException(int limit) {
        super("Daily limit of " + limit + " new words reached");
        this.limit = limit;
    }

    public int limit() {
        return limit;
    }
}
