package dev.words.infra.db;

import dev.words.application.port.LearningProfileRepository;
import dev.words.application.port.LexemeRepository;
import dev.words.application.port.UserCardRepository;
import dev.words.application.port.UserRepository;
import dev.words.domain.CardState;
import dev.words.domain.CefrLevel;
import dev.words.domain.Language;
import dev.words.domain.LearningProfile;
import dev.words.domain.Lexeme;
import dev.words.domain.User;
import dev.words.domain.UserCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserCardRepositoryTest extends RepositoryTest {

    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MICROS);

    @Autowired
    UserRepository users;

    @Autowired
    LearningProfileRepository profiles;

    @Autowired
    LexemeRepository lexemes;

    @Autowired
    UserCardRepository cards;

    long profileId;

    @BeforeEach
    void setUp() {
        var user = users.save(User.register(4001, NOW));
        profileId = profiles.save(LearningProfile.create(user.id(), Language.ENGLISH, Language.RUSSIAN,
                CefrLevel.B1, NOW)).id();
    }

    @Test
    void savesNewCardAndFindsItByLexeme() {
        long lexemeId = lexeme("borrow");

        var saved = cards.save(UserCard.newCard(profileId, lexemeId, NOW));

        assertThat(saved.id()).isNotNull();
        assertThat(cards.findByProfileIdAndLexemeId(profileId, lexemeId)).contains(saved);
        assertThat(cards.countByProfileId(profileId)).isOne();
    }

    @Test
    void sameLexemeCannotBeAddedTwice() {
        long lexemeId = lexeme("borrow");
        cards.save(UserCard.newCard(profileId, lexemeId, NOW));

        assertThatThrownBy(() -> cards.save(UserCard.newCard(profileId, lexemeId, NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void updatesFsrsState() {
        var card = cards.save(UserCard.newCard(profileId, lexeme("borrow"), NOW));

        var reviewed = new UserCard(card.id(), profileId, card.lexemeId(), CardState.REVIEW,
                NOW.plus(Duration.ofDays(3)), 3.17, 5.28, 1, 0, 0, NOW, card.createdAt());
        cards.save(reviewed);

        assertThat(cards.findById(card.id())).contains(reviewed);
    }

    @Test
    void findsDueCardsMostOverdueFirstWithLimit() {
        var dueYesterday = cardDueAt(lexeme("a"), NOW.minus(Duration.ofDays(1)));
        var dueNow = cardDueAt(lexeme("b"), NOW);
        var dueAnHourAgo = cardDueAt(lexeme("c"), NOW.minus(Duration.ofHours(1)));
        cardDueAt(lexeme("d"), NOW.plus(Duration.ofDays(1)));

        assertThat(cards.findDue(profileId, NOW, 10)).containsExactly(dueYesterday, dueAnHourAgo, dueNow);
        assertThat(cards.findDue(profileId, NOW, 2)).containsExactly(dueYesterday, dueAnHourAgo);
    }

    @Test
    void countsCardsCreatedSinceGivenTimeAcrossProfiles() {
        cards.save(UserCard.newCard(profileId, lexeme("old"), NOW.minus(Duration.ofDays(1))));
        cards.save(UserCard.newCard(profileId, lexeme("today-1"), NOW));
        cards.save(UserCard.newCard(profileId, lexeme("today-2"), NOW.plus(Duration.ofHours(1))));

        assertThat(cards.countCreatedSince(List.of(profileId), NOW)).isEqualTo(2);
        assertThat(cards.countCreatedSince(List.of(profileId, 999_999L), NOW)).isEqualTo(2);
        assertThat(cards.countCreatedSince(List.of(), NOW)).isZero();
    }

    @Test
    void pagesThroughDeckNewestFirst() {
        for (int i = 1; i <= 5; i++) {
            cards.save(UserCard.newCard(profileId, lexeme("page-" + i), NOW.plus(Duration.ofMinutes(i))));
        }

        var firstPage = cards.findByProfileId(profileId, 0, 2);
        var lastPage = cards.findByProfileId(profileId, 4, 2);

        assertThat(firstPage).extracting(UserCard::createdAt)
                .containsExactly(NOW.plus(Duration.ofMinutes(5)), NOW.plus(Duration.ofMinutes(4)));
        assertThat(lastPage).extracting(UserCard::createdAt).containsExactly(NOW.plus(Duration.ofMinutes(1)));
    }

    private long lexeme(String text) {
        return lexemes.save(Lexeme.create(Language.ENGLISH, text, null, null, null, Map.of())).id();
    }

    private UserCard cardDueAt(long lexemeId, Instant dueAt) {
        return cards.save(new UserCard(null, profileId, lexemeId, CardState.REVIEW, dueAt, 1, 5, 1, 0, 0, NOW, NOW));
    }
}
