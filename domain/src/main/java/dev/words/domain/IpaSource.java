package dev.words.domain;

/**
 * Where a lexeme's IPA transcription came from, from most to least trusted.
 */
public enum IpaSource {
    WIKTIONARY,
    ESPEAK,
    LLM
}
