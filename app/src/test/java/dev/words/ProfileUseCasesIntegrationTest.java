package dev.words;

import dev.words.application.port.LexemeRepository;
import dev.words.application.port.UserCardRepository;
import dev.words.application.port.UserRepository;
import dev.words.application.profile.CreateProfile;
import dev.words.application.profile.ListProfiles;
import dev.words.application.profile.ProfileSummary;
import dev.words.application.profile.SwitchProfile;
import dev.words.application.user.RegisterUser;
import dev.words.domain.CefrLevel;
import dev.words.domain.Language;
import dev.words.domain.Lexeme;
import dev.words.domain.UserCard;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileUseCasesIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    RegisterUser registerUser;

    @Autowired
    CreateProfile createProfile;

    @Autowired
    SwitchProfile switchProfile;

    @Autowired
    ListProfiles listProfiles;

    @Autowired
    UserRepository users;

    @Autowired
    LexemeRepository lexemes;

    @Autowired
    UserCardRepository cards;

    @Test
    void createsProfilesAndSwitchesBetweenThem() {
        long userId = registerUser.register(6001).id();

        var english = createProfile.create(userId, Language.ENGLISH, Language.RUSSIAN, CefrLevel.B1);
        assertThat(users.findById(userId).orElseThrow().activeProfileId()).isEqualTo(english.id());

        var russian = createProfile.create(userId, Language.RUSSIAN, Language.ENGLISH, CefrLevel.A1);
        assertThat(users.findById(userId).orElseThrow().activeProfileId()).isEqualTo(english.id());

        switchProfile.switchTo(userId, russian.id());
        assertThat(users.findById(userId).orElseThrow().activeProfileId()).isEqualTo(russian.id());
    }

    @Test
    void listsProfilesWithWordCounts() {
        long userId = registerUser.register(6002).id();
        var english = createProfile.create(userId, Language.ENGLISH, Language.RUSSIAN, CefrLevel.B1);
        var russian = createProfile.create(userId, Language.RUSSIAN, Language.ENGLISH, CefrLevel.A1);
        long lexemeId = lexemes.save(Lexeme.create(Language.ENGLISH, "borrow-6002", null, null, null, Map.of())).id();
        cards.save(UserCard.newCard(english.id(), lexemeId, Instant.now()));

        assertThat(listProfiles.list(userId)).containsExactly(
                new ProfileSummary(english, 1, true),
                new ProfileSummary(russian, 0, false));
    }
}
