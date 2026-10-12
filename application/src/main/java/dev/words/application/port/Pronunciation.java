package dev.words.application.port;

import dev.words.domain.IpaSource;

import java.util.Objects;

/**
 * IPA transcription of a word with its origin, e.g. {@code ˈbɒrəʊ} from Wiktionary.
 */
public record Pronunciation(String ipa, IpaSource source) {

    public Pronunciation {
        Objects.requireNonNull(ipa, "ipa");
        Objects.requireNonNull(source, "source");
        if (ipa.isBlank()) {
            throw new IllegalArgumentException("IPA is blank");
        }
        ipa = ipa.strip();
    }
}
