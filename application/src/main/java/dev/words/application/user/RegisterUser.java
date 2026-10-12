package dev.words.application.user;

import dev.words.application.port.UserRepository;
import dev.words.domain.User;

import java.time.Clock;

/**
 * Returns the user with the given Telegram id, creating them on first contact.
 * Safe to call on every incoming message: repeated calls never create a duplicate.
 */
public class RegisterUser {

    private final UserRepository users;
    private final Clock clock;

    public RegisterUser(UserRepository users, Clock clock) {
        this.users = users;
        this.clock = clock;
    }

    public User register(long telegramId) {
        return users.findByTelegramId(telegramId)
                .orElseGet(() -> create(telegramId));
    }

    private User create(long telegramId) {
        try {
            return users.save(User.register(telegramId, clock.instant()));
        } catch (RuntimeException e) {
            // Two concurrent first messages: the other one inserted the user, the unique key rejected ours.
            return users.findByTelegramId(telegramId).orElseThrow(() -> e);
        }
    }
}
