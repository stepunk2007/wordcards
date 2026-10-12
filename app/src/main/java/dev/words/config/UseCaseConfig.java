package dev.words.config;

import dev.words.application.port.UserRepository;
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
}
