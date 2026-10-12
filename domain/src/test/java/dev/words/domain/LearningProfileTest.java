package dev.words.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LearningProfileTest {

    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");

    @Test
    void createsUnsavedProfileWithoutReminder() {
        var profile = LearningProfile.create(1L, Language.ENGLISH, Language.RUSSIAN, CefrLevel.A2, NOW);

        assertThat(profile.id()).isNull();
        assertThat(profile.targetLanguage()).isEqualTo(Language.ENGLISH);
        assertThat(profile.nativeLanguage()).isEqualTo(Language.RUSSIAN);
        assertThat(profile.level()).isEqualTo(CefrLevel.A2);
        assertThat(profile.reminderTime()).isNull();
    }

    @Test
    void rejectsSameTargetAndNativeLanguage() {
        assertThatThrownBy(() -> LearningProfile.create(1L, Language.ENGLISH, Language.ENGLISH, CefrLevel.A1, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changesLevelKeepingOtherFields() {
        var profile = new LearningProfile(5L, 1L, Language.ENGLISH, Language.RUSSIAN, CefrLevel.A2, null, NOW);

        var upgraded = profile.withLevel(CefrLevel.B1);

        assertThat(upgraded.level()).isEqualTo(CefrLevel.B1);
        assertThat(upgraded).usingRecursiveComparison().ignoringFields("level").isEqualTo(profile);
    }
}
