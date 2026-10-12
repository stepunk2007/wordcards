package dev.words.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * A lexeme in the deck of one learning profile, with its FSRS scheduling state.
 *
 * @param stability      FSRS memory stability in days; 0 for a new card
 * @param difficulty     FSRS difficulty from 1 to 10; 0 for a new card
 * @param reps           number of reviews
 * @param lapses         number of times the card was forgotten after being learned
 * @param step           current (re)learning step, see {@link FsrsParameters#learningSteps()}; 0 outside learning
 * @param lastReviewedAt time of the last review, or {@code null} for a new card
 */
public record UserCard(
        Long id,
        Long profileId,
        Long lexemeId,
        CardState state,
        Instant dueAt,
        double stability,
        double difficulty,
        int reps,
        int lapses,
        int step,
        Instant lastReviewedAt,
        Instant createdAt) {

    public UserCard {
        Objects.requireNonNull(profileId, "profileId");
        Objects.requireNonNull(lexemeId, "lexemeId");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(dueAt, "dueAt");
        Objects.requireNonNull(createdAt, "createdAt");
        if (stability < 0 || difficulty < 0 || reps < 0 || lapses < 0 || step < 0) {
            throw new IllegalArgumentException("Card counters and FSRS parameters must not be negative");
        }
    }

    /**
     * A freshly added card that is due for its first review right away.
     */
    public static UserCard newCard(Long profileId, Long lexemeId, Instant now) {
        return new UserCard(null, profileId, lexemeId, CardState.NEW, now, 0, 0, 0, 0, 0, null, now);
    }

    public boolean isDue(Instant now) {
        return !dueAt.isAfter(now);
    }
}
