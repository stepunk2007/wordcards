package dev.words.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserCardTest {

    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");

    @Test
    void newCardIsDueImmediately() {
        var card = UserCard.newCard(1L, 2L, NOW);

        assertThat(card.state()).isEqualTo(CardState.NEW);
        assertThat(card.reps()).isZero();
        assertThat(card.isDue(NOW)).isTrue();
    }

    @Test
    void cardScheduledInFutureIsNotDue() {
        var card = new UserCard(1L, 1L, 2L, CardState.REVIEW, NOW.plus(Duration.ofDays(3)), 3.2, 5.1, 2, 0, NOW);

        assertThat(card.isDue(NOW)).isFalse();
        assertThat(card.isDue(NOW.plus(Duration.ofDays(3)))).isTrue();
    }

    @Test
    void rejectsNegativeCounters() {
        assertThatThrownBy(() -> new UserCard(null, 1L, 2L, CardState.NEW, NOW, 0, 0, -1, 0, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
