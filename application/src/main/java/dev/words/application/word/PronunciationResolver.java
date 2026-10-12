package dev.words.application.word;

import dev.words.application.port.Pronunciation;
import dev.words.application.port.PronunciationPort;
import dev.words.domain.IpaSource;
import dev.words.domain.Language;

import java.util.Optional;

/**
 * Picks the most trustworthy IPA for a new lexeme: the dictionary first, then what the LLM suggested.
 * LLM transcriptions are marked {@link IpaSource#LLM} so they can be shown with a caveat or replaced later.
 */
public class PronunciationResolver {

    private final PronunciationPort pronunciations;

    public PronunciationResolver(PronunciationPort pronunciations) {
        this.pronunciations = pronunciations;
    }

    /**
     * @param lemma dictionary form from the LLM, e.g. "borrow"
     * @param typed what the user typed, e.g. "borrowed"; used when the dictionary has the form but not the lemma
     * @param llmIpa IPA suggested by the LLM, may be {@code null}
     */
    public Optional<Pronunciation> resolve(Language language, String lemma, String typed, String llmIpa) {
        return pronunciations.findIpa(language, lemma)
                .or(() -> typed.equals(lemma) ? Optional.empty() : pronunciations.findIpa(language, typed))
                .or(() -> fromLlm(llmIpa));
    }

    private static Optional<Pronunciation> fromLlm(String llmIpa) {
        if (llmIpa == null || llmIpa.isBlank()) {
            return Optional.empty();
        }
        String ipa = llmIpa.strip();
        if (ipa.length() >= 2 && ipa.startsWith("/") && ipa.endsWith("/")) {
            ipa = ipa.substring(1, ipa.length() - 1).strip();
        }
        return ipa.isEmpty() ? Optional.empty() : Optional.of(new Pronunciation(ipa, IpaSource.LLM));
    }
}
