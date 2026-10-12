package dev.words.application.port;

import dev.words.domain.CefrLevel;

import java.util.Objects;

/**
 * An example sentence produced by the LLM.
 *
 * @param translation translation into the learner's native language
 */
public record GeneratedExample(String text, String translation, CefrLevel level) {

    public GeneratedExample {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(translation, "translation");
        Objects.requireNonNull(level, "level");
        if (text.isBlank()) {
            throw new IllegalArgumentException("Example text is blank");
        }
    }
}
