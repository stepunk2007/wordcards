package dev.words;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class UserCardSchemaTest extends PostgresIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    long profileId;
    long lexemeId;

    @BeforeEach
    void setUp() {
        long userId = jdbc.queryForObject("insert into users (telegram_id) values (2001) returning id", Long.class);
        profileId = jdbc.queryForObject(
                "insert into learning_profile (user_id, target_lang, native_lang, level) values (?, 'en', 'ru', 'B1') returning id",
                Long.class, userId);
        lexemeId = jdbc.queryForObject(
                "insert into lexeme (lang, text, normalized_text) values ('en', 'borrow', 'borrow') returning id",
                Long.class);
    }

    @Test
    void newCardGetsDefaultFsrsState() {
        long cardId = insertCard(profileId, lexemeId);

        var row = jdbc.queryForMap("select state, stability, difficulty, reps, lapses from user_card where id = ?", cardId);
        assertThat(row).containsEntry("state", "NEW")
                .containsEntry("stability", 0.0)
                .containsEntry("difficulty", 0.0)
                .containsEntry("reps", 0)
                .containsEntry("lapses", 0);
    }

    @Test
    void lexemeIsAddedToProfileDeckOnlyOnce() {
        insertCard(profileId, lexemeId);

        assertThatThrownBy(() -> insertCard(profileId, lexemeId)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void storesReviewLog() {
        long cardId = insertCard(profileId, lexemeId);

        jdbc.update("insert into review_log (user_card_id, grade, reviewed_at) values (?, 'GOOD', now())", cardId);

        assertThat(jdbc.queryForObject("select grade from review_log where user_card_id = ?", String.class, cardId))
                .isEqualTo("GOOD");
    }

    @Test
    void rejectsUnknownGrade() {
        long cardId = insertCard(profileId, lexemeId);

        assertThatThrownBy(() -> jdbc.update(
                "insert into review_log (user_card_id, grade, reviewed_at) values (?, 'PERFECT', now())", cardId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsUnknownState() {
        long cardId = insertCard(profileId, lexemeId);

        assertThatThrownBy(() -> jdbc.update("update user_card set state = 'DONE' where id = ?", cardId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingProfileDeletesItsCardsAndReviews() {
        long cardId = insertCard(profileId, lexemeId);
        jdbc.update("insert into review_log (user_card_id, grade, reviewed_at) values (?, 'AGAIN', now())", cardId);

        jdbc.update("delete from learning_profile where id = ?", profileId);

        assertThat(jdbc.queryForObject("select count(*) from review_log", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from lexeme where id = ?", Integer.class, lexemeId)).isOne();
    }

    @Test
    void sharedLexemeInUseCannotBeDeleted() {
        insertCard(profileId, lexemeId);

        assertThatThrownBy(() -> jdbc.update("delete from lexeme where id = ?", lexemeId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private long insertCard(long profileId, long lexemeId) {
        return jdbc.queryForObject(
                "insert into user_card (profile_id, lexeme_id, state, due_at) values (?, ?, 'NEW', now()) returning id",
                Long.class, profileId, lexemeId);
    }
}
