package dev.words.application.port;

import java.util.List;

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

    /**
     * @return new example sentences, not repeating {@link MoreExamplesRequest#existing()}
     * @throws LlmException if the examples could not be generated
     */
    List<GeneratedExample> generateMoreExamples(MoreExamplesRequest request);
}
