package dev.words.config;

import dev.words.application.port.LearningProfileRepository;
import dev.words.application.port.UserRepository;
import dev.words.application.profile.CreateProfile;
import dev.words.application.profile.SwitchProfile;
import dev.words.application.user.RegisterUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Wires framework-free use cases from the application module into the Spring context.
 */
@Configuration
class UseCaseConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    RegisterUser registerUser(UserRepository users, Clock clock) {
        return new RegisterUser(users, clock);
    }

    @Bean
    CreateProfile createProfile(UserRepository users, LearningProfileRepository profiles, Clock clock) {
        return new CreateProfile(users, profiles, clock);
    }

    @Bean
    SwitchProfile switchProfile(UserRepository users, LearningProfileRepository profiles) {
        return new SwitchProfile(users, profiles);
    }
}
