package dev.words.application.port;

/**
 * Generates learning material for a word with a large language model.
 * Calls are slow (seconds) and cost money, so results are cached as shared lexemes.
 */
public interface LlmPort {

    /**
     * @throws WordNotInLanguageException if the text is not a word of {@link WordCardRequest#language()}
     * @throws LlmException               if the card could not be generated
     */
    GeneratedWordCard generateWordCard(WordCardRequest request);
}
