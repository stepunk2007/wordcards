package dev.words.application.profile;

import dev.words.application.error.NotFoundException;
import dev.words.application.fake.InMemoryRepositories;
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

class CreateProfileTest {

    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");

    private final InMemoryRepositories.Users users = new InMemoryRepositories.Users();
    private final InMemoryRepositories.Profiles profiles = new InMemoryRepositories.Profiles();
    private final CreateProfile createProfile =
            new CreateProfile(users, profiles, Clock.fixed(NOW, ZoneOffset.UTC));

    private long userId;

    @BeforeEach
    void setUp() {
        userId = users.save(User.register(42, NOW)).id();
    }

    @Test
    void firstProfileBecomesActive() {
        var profile = createProfile.create(userId, Language.ENGLISH, Language.RUSSIAN, CefrLevel.B1);

        assertThat(profile.id()).isNotNull();
        assertThat(profile.level()).isEqualTo(CefrLevel.B1);
        assertThat(profile.createdAt()).isEqualTo(NOW);
        assertThat(users.findById(userId).orElseThrow().activeProfileId()).isEqualTo(profile.id());
    }

    @Test
    void laterProfileDoesNotChangeActiveOne() {
        var english = createProfile.create(userId, Language.ENGLISH, Language.RUSSIAN, CefrLevel.B1);

        var russian = createProfile.create(userId, Language.RUSSIAN, Language.ENGLISH, CefrLevel.A1);

        assertThat(russian.id()).isNotEqualTo(english.id());
        assertThat(users.findById(userId).orElseThrow().activeProfileId()).isEqualTo(english.id());
    }

    @Test
    void rejectsSecondProfileForSameLanguage() {
        createProfile.create(userId, Language.ENGLISH, Language.RUSSIAN, CefrLevel.B1);

        assertThatThrownBy(() -> createProfile.create(userId, Language.ENGLISH, Language.RUSSIAN, CefrLevel.C1))
                .isInstanceOf(ProfileAlreadyExistsException.class);
        assertThat(profiles.byId).hasSize(1);
    }

    @Test
    void rejectsUnknownUser() {
        assertThatThrownBy(() -> createProfile.create(999, Language.ENGLISH, Language.RUSSIAN, CefrLevel.B1))
                .isInstanceOf(NotFoundException.class);
        assertThat(profiles.byId).isEmpty();
    }
}
