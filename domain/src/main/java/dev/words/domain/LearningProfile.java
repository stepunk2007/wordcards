package dev.words.domain;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Objects;

/**
 * What a user learns: target language, native language for translations and current level.
 * Each profile has its own deck and review schedule.
 *
 * @param reminderTime local time of the daily review reminder, or {@code null} if not set
 */
public record LearningProfile(
        Long id,
        Long userId,
        Language targetLanguage,
        Language nativeLanguage,
        CefrLevel level,
        LocalTime reminderTime,
        Instant createdAt) {

    public LearningProfile {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(targetLanguage, "targetLanguage");
        Objects.requireNonNull(nativeLanguage, "nativeLanguage");
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(createdAt, "createdAt");
        if (targetLanguage == nativeLanguage) {
            throw new IllegalArgumentException("Target and native language must differ: " + targetLanguage);
        }
    }

    public static LearningProfile create(
            Long userId, Language targetLanguage, Language nativeLanguage, CefrLevel level, Instant now) {
        return new LearningProfile(null, userId, targetLanguage, nativeLanguage, level, null, now);
    }

    public LearningProfile withLevel(CefrLevel newLevel) {
        return new LearningProfile(id, userId, targetLanguage, nativeLanguage, newLevel, reminderTime, createdAt);
    }
}
