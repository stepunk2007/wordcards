package dev.words.application.port;

import dev.words.domain.Language;

import java.util.Optional;

/**
 * Looks up how a word or phrase is pronounced.
 */
public interface PronunciationPort {

    /**
     * @param text word or phrase, in any case; implementations normalize it themselves
     * @return IPA transcription, or empty if no source knows the word
     */
    Optional<Pronunciation> findIpa(Language language, String text);
}
