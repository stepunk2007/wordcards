package dev.words.infra.db;

import dev.words.application.port.LexemeRepository;
import dev.words.domain.Language;
import dev.words.domain.Lexeme;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
class JpaLexemeRepository implements LexemeRepository {

    private final LexemeJpaRepository jpa;

    JpaLexemeRepository(LexemeJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public Lexeme save(Lexeme lexeme) {
        return jpa.saveAndFlush(LexemeEntity.fromDomain(lexeme)).toDomain();
    }

    @Override
    public Optional<Lexeme> findById(long id) {
        return jpa.findById(id).map(LexemeEntity::toDomain);
    }

    @Override
    public List<Lexeme> findAllById(Collection<Long> ids) {
        return jpa.findAllById(ids).stream().map(LexemeEntity::toDomain).toList();
    }

    @Override
    public Optional<Lexeme> findByLanguageAndNormalizedText(Language language, String normalizedText) {
        return jpa.findByLanguageAndNormalizedText(language, normalizedText).map(LexemeEntity::toDomain);
    }
}
