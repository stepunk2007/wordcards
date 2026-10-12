package dev.words.infra.db;

import dev.words.domain.Grade;
import dev.words.domain.ReviewLog;
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
@Table(name = "review_log")
class ReviewLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_card_id", nullable = false)
    private Long userCardId;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade", nullable = false)
    private Grade grade;

    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt;

    protected ReviewLogEntity() {
    }

    static ReviewLogEntity fromDomain(ReviewLog log) {
        var entity = new ReviewLogEntity();
        entity.id = log.id();
        entity.userCardId = log.userCardId();
        entity.grade = log.grade();
        entity.reviewedAt = log.reviewedAt();
        return entity;
    }

    ReviewLog toDomain() {
        return new ReviewLog(id, userCardId, grade, reviewedAt);
    }
}
