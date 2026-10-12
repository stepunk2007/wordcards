package dev.words.infra.db;

import dev.words.application.port.LearningProfileRepository;
import dev.words.domain.Language;
import dev.words.domain.LearningProfile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
class JpaLearningProfileRepository implements LearningProfileRepository {

    private final LearningProfileJpaRepository jpa;

    JpaLearningProfileRepository(LearningProfileJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public LearningProfile save(LearningProfile profile) {
        return jpa.saveAndFlush(LearningProfileEntity.fromDomain(profile)).toDomain();
    }

    @Override
    public Optional<LearningProfile> findById(long id) {
        return jpa.findById(id).map(LearningProfileEntity::toDomain);
    }

    @Override
    public List<LearningProfile> findByUserId(long userId) {
        return jpa.findByUserIdOrderByIdAsc(userId).stream().map(LearningProfileEntity::toDomain).toList();
    }

    @Override
    public Optional<LearningProfile> findByUserIdAndTargetLanguage(long userId, Language targetLanguage) {
        return jpa.findByUserIdAndTargetLanguage(userId, targetLanguage).map(LearningProfileEntity::toDomain);
    }
}
