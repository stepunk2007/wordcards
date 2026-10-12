package dev.words;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class UserSchemaTest extends PostgresIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void userCanActivateOwnProfile() {
        long userId = insertUser(1001);
        long profileId = insertProfile(userId, "en", "ru", "B1");

        jdbc.update("update users set active_profile_id = ? where id = ?", profileId, userId);

        assertThat(jdbc.queryForObject("select active_profile_id from users where id = ?", Long.class, userId))
                .isEqualTo(profileId);
    }

    @Test
    void telegramIdIsUnique() {
        insertUser(1002);

        assertThatThrownBy(() -> insertUser(1002)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void oneProfilePerTargetLanguage() {
        long userId = insertUser(1003);
        insertProfile(userId, "en", "ru", "A2");

        assertThatThrownBy(() -> insertProfile(userId, "en", "ru", "B2"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsSameTargetAndNativeLanguage() {
        long userId = insertUser(1004);

        assertThatThrownBy(() -> insertProfile(userId, "en", "en", "A1"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsUnknownLevel() {
        long userId = insertUser(1005);

        assertThatThrownBy(() -> insertProfile(userId, "en", "ru", "D1"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void cannotActivateProfileOfAnotherUser() {
        long owner = insertUser(1006);
        long other = insertUser(1007);
        long profileId = insertProfile(owner, "en", "ru", "B1");

        assertThatThrownBy(() -> jdbc.update("update users set active_profile_id = ? where id = ?", profileId, other))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingActiveProfileClearsIt() {
        long userId = insertUser(1008);
        long profileId = insertProfile(userId, "en", "ru", "B1");
        jdbc.update("update users set active_profile_id = ? where id = ?", profileId, userId);

        jdbc.update("delete from learning_profile where id = ?", profileId);

        assertThat(jdbc.queryForObject("select active_profile_id from users where id = ?", Long.class, userId))
                .isNull();
    }

    private long insertUser(long telegramId) {
        return jdbc.queryForObject("insert into users (telegram_id) values (?) returning id", Long.class, telegramId);
    }

    private long insertProfile(long userId, String targetLang, String nativeLang, String level) {
        return jdbc.queryForObject(
                "insert into learning_profile (user_id, target_lang, native_lang, level) values (?, ?, ?, ?) returning id",
                Long.class, userId, targetLang, nativeLang, level);
    }
}
