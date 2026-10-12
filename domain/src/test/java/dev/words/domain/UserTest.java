package dev.words.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");

    @Test
    void newUserHasNoActiveProfile() {
        var user = User.register(42L, NOW);

        assertThat(user.telegramId()).isEqualTo(42L);
        assertThat(user.hasActiveProfile()).isFalse();
    }

    @Test
    void rejectsNonPositiveTelegramId() {
        assertThatThrownBy(() -> User.register(0L, NOW)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void activatesOwnProfile() {
        var user = new User(1L, 42L, null, NOW);
        var profile = new LearningProfile(7L, 1L, Language.ENGLISH, Language.RUSSIAN, CefrLevel.B1, null, NOW);

        user.activateProfile(profile);

        assertThat(user.activeProfileId()).isEqualTo(7L);
    }

    @Test
    void refusesToActivateForeignOrUnsavedProfile() {
        var user = new User(1L, 42L, null, NOW);
        var foreign = new LearningProfile(7L, 2L, Language.ENGLISH, Language.RUSSIAN, CefrLevel.B1, null, NOW);
        var unsaved = LearningProfile.create(1L, Language.ENGLISH, Language.RUSSIAN, CefrLevel.B1, NOW);

        assertThatThrownBy(() -> user.activateProfile(foreign)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> user.activateProfile(unsaved)).isInstanceOf(IllegalArgumentException.class);
        assertThat(user.hasActiveProfile()).isFalse();
    }
}
