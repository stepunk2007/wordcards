package dev.words.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * A person using the app, identified by their Telegram account.
 * Has at most one active learning profile at a time.
 */
public class User {

    private final Long id;
    private final long telegramId;
    private Long activeProfileId;
    private final Instant createdAt;

    public User(Long id, long telegramId, Long activeProfileId, Instant createdAt) {
        if (telegramId <= 0) {
            throw new IllegalArgumentException("Telegram id must be positive: " + telegramId);
        }
        this.id = id;
        this.telegramId = telegramId;
        this.activeProfileId = activeProfileId;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public static User register(long telegramId, Instant now) {
        return new User(null, telegramId, null, now);
    }

    public void activateProfile(LearningProfile profile) {
        Objects.requireNonNull(profile, "profile");
        if (profile.id() == null) {
            throw new IllegalArgumentException("Profile must be saved before activation");
        }
        if (!Objects.equals(profile.userId(), id)) {
            throw new IllegalArgumentException("Profile " + profile.id() + " belongs to another user");
        }
        this.activeProfileId = profile.id();
    }

    public boolean hasActiveProfile() {
        return activeProfileId != null;
    }

    public Long id() {
        return id;
    }

    public long telegramId() {
        return telegramId;
    }

    public Long activeProfileId() {
        return activeProfileId;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
