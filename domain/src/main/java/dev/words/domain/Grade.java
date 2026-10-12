package dev.words.domain;

/**
 * How well the user recalled a card. The numeric value is the FSRS rating.
 */
public enum Grade {
    AGAIN(1),
    HARD(2),
    GOOD(3),
    EASY(4);

    private final int value;

    Grade(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
