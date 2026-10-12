package dev.words.domain;

/**
 * Learning stage of a card in the FSRS scheduler.
 */
public enum CardState {
    NEW,
    LEARNING,
    REVIEW,
    RELEARNING
}
