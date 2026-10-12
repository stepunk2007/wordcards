package dev.words.config;

import dev.words.application.port.ExampleRepository;
import dev.words.application.port.LearningProfileRepository;
import dev.words.application.port.LexemeRepository;
import dev.words.application.port.LlmPort;
import dev.words.application.port.PronunciationPort;
import dev.words.application.port.UserCardRepository;
import dev.words.application.port.UserRepository;
import dev.words.application.profile.CreateProfile;
import dev.words.application.profile.ListProfiles;
import dev.words.application.profile.SwitchProfile;
import dev.words.application.user.RegisterUser;
import dev.words.application.word.AddWord;
import dev.words.application.word.GetMoreExamples;
import dev.words.application.word.PronunciationResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

/**
 * Wires framework-free use cases from the application module into the Spring context.
 */
@Configuration
class UseCaseConfig {

    /**
     * Ticks in microseconds, the precision of PostgreSQL timestamps, so saved and reloaded values are equal.
     */
    @Bean
    Clock clock() {
        return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000));
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
    ListProfiles listProfiles(UserRepository users, LearningProfileRepository profiles, UserCardRepository cards) {
        return new ListProfiles(users, profiles, cards);
    }

    @Bean
    SwitchProfile switchProfile(UserRepository users, LearningProfileRepository profiles) {
        return new SwitchProfile(users, profiles);
    }

    @Bean
    PronunciationResolver pronunciationResolver(PronunciationPort pronunciations) {
        return new PronunciationResolver(pronunciations);
    }

    @Bean
    AddWord addWord(UserRepository users, LearningProfileRepository profiles, LexemeRepository lexemes,
                    ExampleRepository examples, UserCardRepository cards, LlmPort llm,
                    PronunciationResolver pronunciations, Clock clock) {
        return new AddWord(users, profiles, lexemes, examples, cards, llm, pronunciations, clock);
    }

    @Bean
    GetMoreExamples getMoreExamples(UserRepository users, LearningProfileRepository profiles,
                                    LexemeRepository lexemes, ExampleRepository examples, LlmPort llm) {
        return new GetMoreExamples(users, profiles, lexemes, examples, llm);
    }
}
