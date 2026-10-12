package dev.words.infra.db;

import dev.words.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "users")
class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_id", nullable = false)
    private long telegramId;

    @Column(name = "active_profile_id")
    private Long activeProfileId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserEntity() {
    }

    static UserEntity fromDomain(User user) {
        var entity = new UserEntity();
        entity.id = user.id();
        entity.telegramId = user.telegramId();
        entity.activeProfileId = user.activeProfileId();
        entity.createdAt = user.createdAt();
        return entity;
    }

    User toDomain() {
        return new User(id, telegramId, activeProfileId, createdAt);
    }
}
