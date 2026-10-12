package dev.words.infra.db;

import dev.words.domain.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface LexemeJpaRepository extends JpaRepository<LexemeEntity, Long> {

    Optional<LexemeEntity> findByLanguageAndNormalizedText(Language language, String normalizedText);
}
