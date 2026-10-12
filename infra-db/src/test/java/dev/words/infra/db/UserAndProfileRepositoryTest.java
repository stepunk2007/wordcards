package dev.words.infra.db;

import dev.words.application.port.LearningProfileRepository;
import dev.words.application.port.UserRepository;
import dev.words.domain.CefrLevel;
import dev.words.domain.Language;
import dev.words.domain.LearningProfile;
import dev.words.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserAndProfileRepositoryTest extends RepositoryTest {

    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MICROS);

    @Autowired
    UserRepository users;

    @Autowired
    LearningProfileRepository profiles;

    @Test
    void savesAndFindsUserByTelegramId() {
        var saved = users.save(User.register(3001, NOW));

        assertThat(saved.id()).isNotNull();
        assertThat(users.findByTelegramId(3001)).get()
                .usingRecursiveComparison().isEqualTo(saved);
        assertThat(users.findByTelegramId(3999)).isEmpty();
    }

    @Test
    void duplicateTelegramIdIsRejected() {
        users.save(User.register(3002, NOW));

        assertThatThrownBy(() -> users.save(User.register(3002, NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void savesProfileAndActivatesIt() {
        var user = users.save(User.register(3003, NOW));
        var profile = profiles.save(LearningProfile.create(user.id(), Language.ENGLISH, Language.RUSSIAN,
                CefrLevel.B1, NOW));

        user.activateProfile(profile);
        users.save(user);

        assertThat(users.findById(user.id()).orElseThrow().activeProfileId()).isEqualTo(profile.id());
        assertThat(profiles.findById(profile.id())).contains(profile);
    }

    @Test
    void updatesProfileLevelAndReminder() {
        var user = users.save(User.register(3004, NOW));
        var profile = profiles.save(LearningProfile.create(user.id(), Language.ENGLISH, Language.RUSSIAN,
                CefrLevel.A2, NOW));

        var changed = new LearningProfile(profile.id(), user.id(), Language.ENGLISH, Language.RUSSIAN,
                CefrLevel.B2, LocalTime.of(9, 30), NOW);
        profiles.save(changed);

        assertThat(profiles.findById(profile.id())).contains(changed);
    }

    @Test
    void findsProfilesOfUserByTargetLanguage() {
        var user = users.save(User.register(3005, NOW));
        var english = profiles.save(LearningProfile.create(user.id(), Language.ENGLISH, Language.RUSSIAN,
                CefrLevel.B1, NOW));
        var russian = profiles.save(LearningProfile.create(user.id(), Language.RUSSIAN, Language.ENGLISH,
                CefrLevel.A1, NOW));

        assertThat(profiles.findByUserId(user.id())).containsExactly(english, russian);
        assertThat(profiles.findByUserIdAndTargetLanguage(user.id(), Language.RUSSIAN)).contains(russian);
    }
}
