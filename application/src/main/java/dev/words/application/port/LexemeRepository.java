package dev.words.application.port;

import dev.words.domain.Language;
import dev.words.domain.Lexeme;

import java.util.Optional;

/**
 * Lexemes shared by all users.
 */
public interface LexemeRepository {

    Lexeme save(Lexeme lexeme);

    Optional<Lexeme> findById(long id);

    Optional<Lexeme> findByLanguageAndNormalizedText(Language language, String normalizedText);
}
