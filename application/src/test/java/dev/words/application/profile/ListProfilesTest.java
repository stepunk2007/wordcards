package dev.words.application.profile;

import dev.words.application.error.NotFoundException;
import dev.words.application.fake.InMemoryRepositories;
import dev.words.domain.CefrLevel;
import dev.words.domain.Language;
import dev.words.domain.User;
import dev.words.domain.UserCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListProfilesTest {

    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");

    private final InMemoryRepositories.Users users = new InMemoryRepositories.Users();
    private final InMemoryRepositories.Profiles profiles = new InMemoryRepositories.Profiles();
    private final InMemoryRepositories.Cards cards = new InMemoryRepositories.Cards();
    private final CreateProfile createProfile =
            new CreateProfile(users, profiles, Clock.fixed(NOW, ZoneOffset.UTC));
    private final ListProfiles listProfiles = new ListProfiles(users, profiles, cards);

    private long userId;

    @BeforeEach
    void setUp() {
        userId = users.save(User.register(42, NOW)).id();
    }

    @Test
    void listsProfilesWithWordCountsAndActiveFlag() {
        var english = createProfile.create(userId, Language.ENGLISH, Language.RUSSIAN, CefrLevel.B1);
        var russian = createProfile.create(userId, Language.RUSSIAN, Language.ENGLISH, CefrLevel.A1);
        cards.save(UserCard.newCard(english.id(), 1L, NOW));
        cards.save(UserCard.newCard(english.id(), 2L, NOW));
        cards.save(UserCard.newCard(russian.id(), 3L, NOW));

        assertThat(listProfiles.list(userId)).containsExactly(
                new ProfileSummary(english, 2, true),
                new ProfileSummary(russian, 1, false));
    }

    @Test
    void emptyDeckHasZeroWords() {
        var english = createProfile.create(userId, Language.ENGLISH, Language.RUSSIAN, CefrLevel.B1);

        assertThat(listProfiles.list(userId)).containsExactly(new ProfileSummary(english, 0, true));
    }

    @Test
    void userWithoutProfilesGetsEmptyList() {
        assertThat(listProfiles.list(userId)).isEmpty();
    }

    @Test
    void doesNotShowProfilesOfOtherUsers() {
        long otherUserId = users.save(User.register(43, NOW)).id();
        createProfile.create(otherUserId, Language.ENGLISH, Language.RUSSIAN, CefrLevel.B1);

        assertThat(listProfiles.list(userId)).isEmpty();
    }

    @Test
    void rejectsUnknownUser() {
        assertThatThrownBy(() -> listProfiles.list(999)).isInstanceOf(NotFoundException.class);
    }
}
