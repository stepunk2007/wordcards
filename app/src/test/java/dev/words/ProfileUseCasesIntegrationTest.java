package dev.words;

import dev.words.application.port.UserRepository;
import dev.words.application.profile.CreateProfile;
import dev.words.application.profile.SwitchProfile;
import dev.words.application.user.RegisterUser;
import dev.words.domain.CefrLevel;
import dev.words.domain.Language;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileUseCasesIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    RegisterUser registerUser;

    @Autowired
    CreateProfile createProfile;

    @Autowired
    SwitchProfile switchProfile;

    @Autowired
    UserRepository users;

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
}
