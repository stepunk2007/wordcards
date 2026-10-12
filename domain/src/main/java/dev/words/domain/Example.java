package dev.words.domain;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A simple sentence using a lexeme, shared by all users, with translations per native language.
 */
public record Example(
        Long id,
        Long lexemeId,
        String text,
        CefrLevel level,
        Map<Language, String> translations) {

    public Example {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(level, "level");
        if (text.isBlank()) {
            throw new IllegalArgumentException("Example text is blank");
        }
        translations = Map.copyOf(translations == null ? Map.of() : translations);
    }

    public Optional<String> translationIn(Language nativeLanguage) {
        return Optional.ofNullable(translations.get(nativeLanguage));
    }
}
