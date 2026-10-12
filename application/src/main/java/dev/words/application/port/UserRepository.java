package dev.words.application.port;

import dev.words.domain.User;

import java.util.Optional;

public interface UserRepository {

    /**
     * Inserts a new user or updates an existing one.
     *
     * @return the saved user with its id assigned
     */
    User save(User user);

    Optional<User> findById(long id);

    Optional<User> findByTelegramId(long telegramId);
}
