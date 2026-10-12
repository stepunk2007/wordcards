package dev.words.application.port;

import java.util.List;
import java.util.Objects;

/**
 * A word card produced by the LLM.
 *
 * @param lemma        dictionary form of the word, e.g. "borrow" for "borrowed"
 * @param partOfSpeech e.g. "verb", "noun", "phrasal verb"
 * @param ipa          IPA of the lemma without slashes, or {@code null}; a fallback when the dictionary lacks the word
 * @param meaning      short meaning in the learner's native language
 * @param examples     simple sentences using the word, with translations
 */
public record GeneratedWordCard(
        String lemma,
        String partOfSpeech,
        String ipa,
        String meaning,
        List<GeneratedExample> examples) {

    public GeneratedWordCard {
        Objects.requireNonNull(lemma, "lemma");
        Objects.requireNonNull(meaning, "meaning");
        ipa = ipa == null || ipa.isBlank() ? null : ipa.strip();
        examples = List.copyOf(examples == null ? List.of() : examples);
        if (lemma.isBlank()) {
            throw new IllegalArgumentException("Lemma is blank");
        }
    }
}
