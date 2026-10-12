package dev.words.application.port;

import dev.words.domain.CefrLevel;
import dev.words.domain.Language;

import java.util.List;
import java.util.Objects;

/**
 * Asks for extra example sentences for a word that already has some.
 *
 * @param existing sentences the word already has; new ones must differ from them
 * @param count    how many new sentences to write
 */
public record MoreExamplesRequest(
        String lemma,
        Language language,
        Language nativeLanguage,
        CefrLevel level,
        List<String> existing,
        int count) {

    public MoreExamplesRequest {
        Objects.requireNonNull(lemma, "lemma");
        Objects.requireNonNull(language, "language");
        Objects.requireNonNull(nativeLanguage, "nativeLanguage");
        Objects.requireNonNull(level, "level");
        existing = List.copyOf(existing == null ? List.of() : existing);
        if (count <= 0) {
            throw new IllegalArgumentException("Count must be positive: " + count);
        }
    }
}
