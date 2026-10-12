package dev.words.infra.db;

import dev.words.domain.CardState;
import dev.words.domain.UserCard;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "user_card")
class UserCardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Column(name = "lexeme_id", nullable = false)
    private Long lexemeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private CardState state;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "stability", nullable = false)
    private double stability;

    @Column(name = "difficulty", nullable = false)
    private double difficulty;

    @Column(name = "reps", nullable = false)
    private int reps;

    @Column(name = "lapses", nullable = false)
    private int lapses;

    @Column(name = "step", nullable = false)
    private int step;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserCardEntity() {
    }

    static UserCardEntity fromDomain(UserCard card) {
        var entity = new UserCardEntity();
        entity.id = card.id();
        entity.profileId = card.profileId();
        entity.lexemeId = card.lexemeId();
        entity.state = card.state();
        entity.dueAt = card.dueAt();
        entity.stability = card.stability();
        entity.difficulty = card.difficulty();
        entity.reps = card.reps();
        entity.lapses = card.lapses();
        entity.step = card.step();
        entity.lastReviewedAt = card.lastReviewedAt();
        entity.createdAt = card.createdAt();
        return entity;
    }

    UserCard toDomain() {
        return new UserCard(id, profileId, lexemeId, state, dueAt, stability, difficulty, reps, lapses, step,
                lastReviewedAt, createdAt);
    }
}
