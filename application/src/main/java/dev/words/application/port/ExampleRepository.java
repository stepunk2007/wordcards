package dev.words.application.port;

import dev.words.domain.Example;

import java.util.List;

/**
 * Example sentences shared by all users.
 */
public interface ExampleRepository {

    List<Example> saveAll(List<Example> examples);

    /**
     * @return examples of the lexeme in the order they were added
     */
    List<Example> findByLexemeId(long lexemeId);
}
