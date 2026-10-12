package dev.words.application.port;

import dev.words.domain.CefrLevel;
import dev.words.domain.Language;

import java.util.Objects;

/**
 * What to generate a word card for.
 *
 * @param text           word or phrase as typed by the user
 * @param language       language of the word
 * @param nativeLanguage language of the meaning and example translations
 * @param level          learner's level; examples should be at or below it
 */
public record WordCardRequest(String text, Language language, Language nativeLanguage, CefrLevel level) {

    public WordCardRequest {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(language, "language");
        Objects.requireNonNull(nativeLanguage, "nativeLanguage");
        Objects.requireNonNull(level, "level");
        if (text.isBlank()) {
            throw new IllegalArgumentException("Word text is blank");
        }
    }
}
