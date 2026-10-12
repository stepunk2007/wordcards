package dev.words.infra.db;

import dev.words.domain.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface LearningProfileJpaRepository extends JpaRepository<LearningProfileEntity, Long> {

    List<LearningProfileEntity> findByUserIdOrderByIdAsc(Long userId);

    Optional<LearningProfileEntity> findByUserIdAndTargetLanguage(Long userId, Language targetLanguage);
}
