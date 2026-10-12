package dev.words.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * One review of a card: what the user answered and when. Kept for statistics and for tuning FSRS later.
 */
public record ReviewLog(Long id, Long userCardId, Grade grade, Instant reviewedAt) {

    public ReviewLog {
        Objects.requireNonNull(userCardId, "userCardId");
        Objects.requireNonNull(grade, "grade");
        Objects.requireNonNull(reviewedAt, "reviewedAt");
    }
}
