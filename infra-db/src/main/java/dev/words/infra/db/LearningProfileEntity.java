package dev.words.infra.db;

import dev.words.domain.CefrLevel;
import dev.words.domain.Language;
import dev.words.domain.LearningProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "learning_profile")
class LearningProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "target_lang", nullable = false)
    private Language targetLanguage;

    @Column(name = "native_lang", nullable = false)
    private Language nativeLanguage;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private CefrLevel level;

    @Column(name = "reminder_time")
    private LocalTime reminderTime;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LearningProfileEntity() {
    }

    static LearningProfileEntity fromDomain(LearningProfile profile) {
        var entity = new LearningProfileEntity();
        entity.id = profile.id();
        entity.userId = profile.userId();
        entity.targetLanguage = profile.targetLanguage();
        entity.nativeLanguage = profile.nativeLanguage();
        entity.level = profile.level();
        entity.reminderTime = profile.reminderTime();
        entity.createdAt = profile.createdAt();
        return entity;
    }

    LearningProfile toDomain() {
        return new LearningProfile(id, userId, targetLanguage, nativeLanguage, level, reminderTime, createdAt);
    }
}
