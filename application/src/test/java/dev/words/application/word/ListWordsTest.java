package dev.words.application.word;

import dev.words.application.error.NoActiveProfileException;
import dev.words.application.fake.InMemoryRepositories;
import dev.words.application.profile.CreateProfile;
import dev.words.domain.CefrLevel;
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

class ListWordsTest {

    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");

    private final InMemoryRepositories.Users users = new InMemoryRepositories.Users();
    private final InMemoryRepositories.Profiles profiles = new InMemoryRepositories.Profiles();
    private final InMemoryRepositories.Lexemes lexemes = new InMemoryRepositories.Lexemes();
    private final InMemoryRepositories.Cards cards = new InMemoryRepositories.Cards();
    private final ListWords listWords = new ListWords(users, profiles, cards, lexemes);

    private long userId;
    private LearningProfile english;

    @BeforeEach
    void setUp() {
        userId = users.save(User.register(42, NOW)).id();
        english = new CreateProfile(users, profiles, Clock.fixed(NOW, ZoneOffset.UTC))
                .create(userId, Language.ENGLISH, Language.RUSSIAN, CefrLevel.A2);
    }

    @Test
    void listsNewestWordsFirstPageByPage() {
        for (int i = 1; i <= 5; i++) {
            addCard(english, "word" + i, NOW.plus(Duration.ofMinutes(i)));
        }

        var first = listWords.list(userId, 0, 2);
        var last = listWords.list(userId, 2, 2);

        assertThat(first.words()).extracting(Lexeme::text).containsExactly("word5", "word4");
        assertThat(first.totalWords()).isEqualTo(5);
        assertThat(first.totalPages()).isEqualTo(3);
        assertThat(first.hasPrevious()).isFalse();
        assertThat(first.hasNext()).isTrue();
        assertThat(last.words()).extracting(Lexeme::text).containsExactly("word1");
        assertThat(last.hasNext()).isFalse();
    }

    @Test
    void pagePastTheEndShowsTheLastPage() {
        addCard(english, "borrow", NOW);

        var page = listWords.list(userId, 7, 10);

        assertThat(page.page()).isZero();
        assertThat(page.words()).extracting(Lexeme::text).containsExactly("borrow");
    }

    @Test
    void emptyDeckHasOneEmptyPage() {
        var page = listWords.list(userId, 0, 10);

        assertThat(page.words()).isEmpty();
        assertThat(page.totalPages()).isOne();
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    void showsOnlyTheActiveProfile() {
        addCard(english, "borrow", NOW);
        var russian = new CreateProfile(users, profiles, Clock.fixed(NOW, ZoneOffset.UTC))
                .create(userId, Language.RUSSIAN, Language.ENGLISH, CefrLevel.A1);
        addCard(russian, "книга", NOW);

        assertThat(listWords.list(userId, 0, 10).words()).extracting(Lexeme::text).containsExactly("borrow");
    }

    @Test
    void userWithoutProfileGetsError() {
        long newcomer = users.save(User.register(43, NOW)).id();

        assertThatThrownBy(() -> listWords.list(newcomer, 0, 10)).isInstanceOf(NoActiveProfileException.class);
    }

    private void addCard(LearningProfile profile, String text, Instant createdAt) {
        var lexeme = lexemes.save(Lexeme.create(profile.targetLanguage(), text, null, null, null,
                Map.of(profile.nativeLanguage(), "перевод")));
        cards.save(UserCard.newCard(profile.id(), lexeme.id(), createdAt));
    }
}
