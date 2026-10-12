package dev.words.application.error;

/**
 * The requested user, profile or card does not exist or belongs to someone else.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
