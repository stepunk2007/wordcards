package dev.words.application.port;

/**
 * Generates learning material for a word with a large language model.
 * Calls are slow (seconds) and cost money, so results are cached as shared lexemes.
 */
public interface LlmPort {

    GeneratedWordCard generateWordCard(WordCardRequest request);
}
