package dev.words.application.profile;

import dev.words.application.error.NotFoundException;
import dev.words.domain.CefrLevel;
import dev.words.domain.Language;
import dev.words.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SwitchProfileTest {

    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");

    private final InMemoryRepositories.Users users = new InMemoryRepositories.Users();
    private final InMemoryRepositories.Profiles profiles = new InMemoryRepositories.Profiles();
    private final CreateProfile createProfile =
            new CreateProfile(users, profiles, Clock.fixed(NOW, ZoneOffset.UTC));
    private final SwitchProfile switchProfile = new SwitchProfile(users, profiles);

    private long userId;

    @BeforeEach
    void setUp() {
        userId = users.save(User.register(42, NOW)).id();
    }

    @Test
    void switchesActiveProfile() {
        createProfile.create(userId, Language.ENGLISH, Language.RUSSIAN, CefrLevel.B1);
        var russian = createProfile.create(userId, Language.RUSSIAN, Language.ENGLISH, CefrLevel.A1);

        var active = switchProfile.switchTo(userId, russian.id());

        assertThat(active).isEqualTo(russian);
        assertThat(users.findById(userId).orElseThrow().activeProfileId()).isEqualTo(russian.id());
    }

    @Test
    void switchingToAlreadyActiveProfileSavesNothing() {
        var english = createProfile.create(userId, Language.ENGLISH, Language.RUSSIAN, CefrLevel.B1);
        int savesBefore = users.saves;

        switchProfile.switchTo(userId, english.id());

        assertThat(users.saves).isEqualTo(savesBefore);
    }

    @Test
    void cannotSwitchToProfileOfAnotherUser() {
        long otherUserId = users.save(User.register(43, NOW)).id();
        var foreign = createProfile.create(otherUserId, Language.ENGLISH, Language.RUSSIAN, CefrLevel.B1);

        assertThatThrownBy(() -> switchProfile.switchTo(userId, foreign.id()))
                .isInstanceOf(NotFoundException.class);
        assertThat(users.findById(userId).orElseThrow().hasActiveProfile()).isFalse();
    }

    @Test
    void rejectsUnknownProfileOrUser() {
        assertThatThrownBy(() -> switchProfile.switchTo(userId, 12345)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> switchProfile.switchTo(999, 100)).isInstanceOf(NotFoundException.class);
    }
}
