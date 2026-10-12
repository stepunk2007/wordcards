package dev.words.application.review;

import dev.words.application.error.NoActiveProfileException;
import dev.words.application.error.NotFoundException;
import dev.words.application.fake.InMemoryRepositories;
import dev.words.application.profile.CreateProfile;
import dev.words.domain.CardState;
import dev.words.domain.CefrLevel;
import dev.words.domain.FsrsScheduler;
import dev.words.domain.Grade;
import dev.words.domain.Language;
import dev.words.domain.LearningProfile;
import dev.words.domain.Lexeme;
import dev.words.domain.User;
import dev.words.domain.UserCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final InMemoryRepositories.Users users = new InMemoryRepositories.Users();
    private final InMemoryRepositories.Profiles profiles = new InMemoryRepositories.Profiles();
    private final InMemoryRepositories.Lexemes lexemes = new InMemoryRepositories.Lexemes();
    private final InMemoryRepositories.Cards cards = new InMemoryRepositories.Cards();
    private final InMemoryRepositories.ReviewLogs reviewLogs = new InMemoryRepositories.ReviewLogs();
    private final GetDueCards getDueCards = new GetDueCards(users, profiles, cards, lexemes, CLOCK);
    private final ReviewCard reviewCard = new ReviewCard(profiles, cards, reviewLogs, new FsrsScheduler(), CLOCK);

    private long userId;
    private LearningProfile english;

    @BeforeEach
    void setUp() {
        userId = users.save(User.register(42, NOW)).id();
        english = new CreateProfile(users, profiles, CLOCK)
                .create(userId, Language.ENGLISH, Language.RUSSIAN, CefrLevel.A2);
    }

    @Test
    void dueCardsComeMostOverdueFirstWithTheirWords() {
        card(english, "later", NOW.plus(Duration.ofDays(1)));
        card(english, "today", NOW);
        card(english, "yesterday", NOW.minus(Duration.ofDays(1)));

        var due = getDueCards.due(userId, 10);

        assertThat(due.cards()).extracting(c -> c.lexeme().text()).containsExactly("yesterday", "today");
        assertThat(due.totalDue()).isEqualTo(2);
        assertThat(due.profile()).isEqualTo(english);
    }

    @Test
    void limitCutsTheListButTotalCountsAll() {
        for (int i = 0; i < 5; i++) {
            card(english, "word" + i, NOW.minus(Duration.ofMinutes(i)));
        }

        var due = getDueCards.due(userId, 2);

        assertThat(due.cards()).hasSize(2);
        assertThat(due.totalDue()).isEqualTo(5);
    }

    @Test
    void onlyActiveProfileIsReviewed() {
        card(english, "borrow", NOW);
        var russian = new CreateProfile(users, profiles, CLOCK)
                .create(userId, Language.RUSSIAN, Language.ENGLISH, CefrLevel.A1);
        card(russian, "книга", NOW);

        assertThat(getDueCards.due(userId, 10).cards()).extracting(c -> c.lexeme().text())
                .containsExactly("borrow");
    }

    @Test
    void userWithoutProfileGetsError() {
        long newcomer = users.save(User.register(43, NOW)).id();

        assertThatThrownBy(() -> getDueCards.due(newcomer, 10)).isInstanceOf(NoActiveProfileException.class);
    }

    @Test
    void reviewSchedulesWithFsrsAndWritesLog() {
        var card = card(english, "borrow", NOW);

        var reviewed = reviewCard.review(userId, card.id(), Grade.GOOD);

        assertThat(reviewed.state()).isEqualTo(CardState.LEARNING);
        assertThat(reviewed.dueAt()).isEqualTo(NOW.plus(Duration.ofMinutes(10)));
        assertThat(reviewed.reps()).isOne();
        assertThat(cards.findById(card.id())).contains(reviewed);
        assertThat(reviewLogs.findByUserCardId(card.id())).singleElement().satisfies(log -> {
            assertThat(log.grade()).isEqualTo(Grade.GOOD);
            assertThat(log.reviewedAt()).isEqualTo(NOW);
        });
        assertThat(getDueCards.due(userId, 10).cards()).isEmpty();
    }

    @Test
    void cannotReviewCardOfAnotherUser() {
        var card = card(english, "borrow", NOW);
        long stranger = users.save(User.register(43, NOW)).id();

        assertThatThrownBy(() -> reviewCard.review(stranger, card.id(), Grade.EASY))
                .isInstanceOf(NotFoundException.class);
        assertThat(reviewLogs.all).isEmpty();
        assertThat(cards.findById(card.id())).contains(card);
    }

    @Test
    void unknownCardIsNotFound() {
        assertThatThrownBy(() -> reviewCard.review(userId, 999, Grade.GOOD)).isInstanceOf(NotFoundException.class);
    }

    private UserCard card(LearningProfile profile, String text, Instant dueAt) {
        var lexeme = lexemes.save(Lexeme.create(profile.targetLanguage(), text, null, null, null,
                Map.of(profile.nativeLanguage(), "перевод")));
        return cards.save(new UserCard(null, profile.id(), lexeme.id(), CardState.NEW, dueAt, 0, 0, 0, 0, 0, null,
                NOW.minus(Duration.ofDays(2))));
    }
}
