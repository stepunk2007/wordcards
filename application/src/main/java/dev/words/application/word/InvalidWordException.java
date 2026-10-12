package dev.words.application.word;

/**
 * The text cannot be a word or phrase: empty or too long.
 */
public class InvalidWordException extends RuntimeException {

    public InvalidWordException(String message) {
        super(message);
    }
}
