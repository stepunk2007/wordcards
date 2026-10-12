package dev.words.application.profile;

import dev.words.domain.Language;

/**
 * The user already learns this language; a user has at most one profile per target language.
 */
public class ProfileAlreadyExistsException extends RuntimeException {

    private final Language targetLanguage;

    public ProfileAlreadyExistsException(Language targetLanguage) {
        super("Profile for " + targetLanguage + " already exists");
        this.targetLanguage = targetLanguage;
    }

    public Language targetLanguage() {
        return targetLanguage;
    }
}
