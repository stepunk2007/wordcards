package dev.words.application.error;

/**
 * The user has not chosen a language to learn yet, so there is no deck to add words to.
 */
public class NoActiveProfileException extends RuntimeException {

    public NoActiveProfileException(long userId) {
        super("User " + userId + " has no active learning profile");
    }
}
