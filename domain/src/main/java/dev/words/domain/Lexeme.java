package dev.words.domain;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A word or phrase in a given language, shared by all users so that it is generated only once.
 *
 * @param ipa         IPA transcription, or {@code null} if unknown
 * @param ipaSource   origin of {@code ipa}; {@code null} exactly when {@code ipa} is {@code null}
 * @param meanings    short meaning per native language of the learner
 */
public record Lexeme(
        Long id,
        Language language,
        String text,
        String normalizedText,
        String partOfSpeech,
        String ipa,
        IpaSource ipaSource,
        Map<Language, String> meanings) {

    public Lexeme {
        Objects.requireNonNull(language, "language");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(normalizedText, "normalizedText");
        if ((ipa == null) != (ipaSource == null)) {
            throw new IllegalArgumentException("IPA and its source must be set together");
        }
        meanings = Map.copyOf(meanings == null ? Map.of() : meanings);
    }

    public static Lexeme create(
            Language language, String text, String partOfSpeech, String ipa, IpaSource ipaSource,
            Map<Language, String> meanings) {
        String stripped = text == null ? null : text.strip();
        return new Lexeme(null, language, stripped, WordNormalizer.normalize(text), partOfSpeech, ipa, ipaSource,
                meanings);
    }

    public Optional<String> meaningIn(Language nativeLanguage) {
        return Optional.ofNullable(meanings.get(nativeLanguage));
    }
}
