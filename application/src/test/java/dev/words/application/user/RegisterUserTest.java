package dev.words.application.user;

import dev.words.application.port.UserRepository;
import dev.words.domain.User;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegisterUserTest {

    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final RegisterUser registerUser = new RegisterUser(users, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void createsUserOnFirstContact() {
        var user = registerUser.register(42);

        assertThat(user.id()).isNotNull();
        assertThat(user.telegramId()).isEqualTo(42);
        assertThat(user.createdAt()).isEqualTo(NOW);
        assertThat(user.hasActiveProfile()).isFalse();
    }

    @Test
    void repeatedCallReturnsSameUserWithoutDuplicate() {
        var first = registerUser.register(42);
        var second = registerUser.register(42);

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(users.saves).isOne();
    }

    @Test
    void returnsUserCreatedConcurrentlyWhenInsertIsRejected() {
        users.insertConcurrentlyOnNextSave = true;

        var user = registerUser.register(42);

        assertThat(user.telegramId()).isEqualTo(42);
        assertThat(users.byTelegramId).hasSize(1);
    }

    @Test
    void rethrowsSaveErrorIfUserStillMissing() {
        users.failOnSave = true;

        assertThatThrownBy(() -> registerUser.register(42)).isInstanceOf(IllegalStateException.class);
    }

    private static class InMemoryUserRepository implements UserRepository {

        final Map<Long, User> byTelegramId = new HashMap<>();
        long nextId = 1;
        int saves;
        boolean insertConcurrentlyOnNextSave;
        boolean failOnSave;

        @Override
        public User save(User user) {
            if (failOnSave) {
                throw new IllegalStateException("database is down");
            }
            if (insertConcurrentlyOnNextSave) {
                insertConcurrentlyOnNextSave = false;
                store(user);
                throw new IllegalStateException("duplicate key");
            }
            saves++;
            return store(user);
        }

        private User store(User user) {
            var saved = new User(nextId++, user.telegramId(), user.activeProfileId(), user.createdAt());
            byTelegramId.put(saved.telegramId(), saved);
            return saved;
        }

        @Override
        public Optional<User> findById(long id) {
            return byTelegramId.values().stream().filter(user -> user.id() == id).findFirst();
        }

        @Override
        public Optional<User> findByTelegramId(long telegramId) {
            return Optional.ofNullable(byTelegramId.get(telegramId));
        }
    }
}
