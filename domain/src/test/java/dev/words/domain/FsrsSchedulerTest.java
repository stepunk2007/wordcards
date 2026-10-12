package dev.words.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Expected values come from the reference implementation py-fsrs 6.3.2 with default parameters and fuzzing off
 * ({@code Scheduler(enable_fuzzing=False)}), replaying the same reviews.
 */
class FsrsSchedulerTest {

    private static final Instant START = Instant.parse("2026-10-12T10:00:00Z");
    private static final double EPS = 1e-9;

    private final FsrsScheduler scheduler = new FsrsScheduler();

    @Test
    void goodEveryTimeGraduatesAndGrowsIntervals() {
        new Replay()
                .review(Grade.GOOD, CardState.LEARNING, 1, 2.3065, 2.118103970459016, Duration.ofMinutes(10))
                .review(Grade.GOOD, CardState.REVIEW, 0, 2.3065, 2.111214235785395, Duration.ofDays(2))
                .review(Grade.GOOD, CardState.REVIEW, 0, 10.971048263078135, 2.1043313908464483, Duration.ofDays(11))
                .review(Grade.GOOD, CardState.REVIEW, 0, 46.316858440073425, 2.0974554287524403, Duration.ofDays(46))
                .review(Grade.GOOD, CardState.REVIEW, 0, 162.99981577472244, 2.0905863426205262, Duration.ofDays(163));
    }

    @Test
    void easyOnFirstReviewSkipsLearning() {
        new Replay()
                .review(Grade.EASY, CardState.REVIEW, 0, 8.2956, 1.0, Duration.ofDays(8))
                .review(Grade.GOOD, CardState.REVIEW, 0, 38.90514998232684, 1.0, Duration.ofDays(39));
    }

    @Test
    void againThenGoodWalksThroughLearningSteps() {
        new Replay()
                .review(Grade.AGAIN, CardState.LEARNING, 0, 0.212, 6.4133, Duration.ofMinutes(1))
                .review(Grade.GOOD, CardState.LEARNING, 1, 0.24668918777567272, 6.402115069296838, Duration.ofMinutes(10))
                .review(Grade.GOOD, CardState.REVIEW, 0, 0.2842063592758949, 6.3909413235243795, Duration.ofDays(1));
    }

    @Test
    void hardOnFirstStepWaitsBetweenTheTwoSteps() {
        new Replay()
                .review(Grade.HARD, CardState.LEARNING, 0, 1.2931, 5.112170705601056, Duration.ofSeconds(330))
                .review(Grade.HARD, CardState.LEARNING, 0, 1.2931, 6.7404595108297, Duration.ofSeconds(330))
                .review(Grade.GOOD, CardState.LEARNING, 1, 1.3358997622047517, 6.728947420615708, Duration.ofMinutes(10));
    }

    @Test
    void forgettingALearnedCardRelearnsIt() {
        var card = new Replay()
                .review(Grade.GOOD, CardState.LEARNING, 1, 2.3065, 2.118103970459016, Duration.ofMinutes(10))
                .review(Grade.GOOD, CardState.REVIEW, 0, 2.3065, 2.111214235785395, Duration.ofDays(2))
                .review(Grade.GOOD, CardState.REVIEW, 0, 10.971048263078135, 2.1043313908464483, Duration.ofDays(11))
                .review(Grade.AGAIN, CardState.RELEARNING, 0, 1.53901253028147, 7.389975788014609, Duration.ofMinutes(10))
                .review(Grade.GOOD, CardState.REVIEW, 0, 1.5718415897918614, 7.377814181523433, Duration.ofDays(2))
                .review(Grade.GOOD, CardState.REVIEW, 0, 4.934768723890772, 7.365664736638748, Duration.ofDays(5))
                .card;

        assertThat(card.lapses()).isOne();
        assertThat(card.reps()).isEqualTo(6);
    }

    @Test
    void lateReviewGrowsStabilityMore() {
        new Replay()
                .review(Grade.GOOD, CardState.LEARNING, 1, 2.3065, 2.118103970459016, Duration.ofMinutes(10))
                .review(Grade.GOOD, CardState.REVIEW, 0, 2.3065, 2.111214235785395, Duration.ofDays(2))
                .reviewAfter(Duration.ofDays(30), Grade.GOOD, CardState.REVIEW, 0, 10.971048263078135,
                        2.1043313908464483, Duration.ofDays(11))
                .review(Grade.HARD, CardState.REVIEW, 0, 50.90033717820445, 4.743715608383524, Duration.ofDays(51));
    }

    @Test
    void reviewRecordsTimeAndKeepsIdentity() {
        var card = new UserCard(9L, 3L, 4L, CardState.NEW, START, 0, 0, 0, 0, 0, null, START);

        var reviewed = scheduler.review(card, Grade.GOOD, START);

        assertThat(reviewed.id()).isEqualTo(9L);
        assertThat(reviewed.profileId()).isEqualTo(3L);
        assertThat(reviewed.lexemeId()).isEqualTo(4L);
        assertThat(reviewed.lastReviewedAt()).isEqualTo(START);
        assertThat(reviewed.createdAt()).isEqualTo(START);
    }

    @Test
    void retrievabilityIsNinetyPercentAfterStabilityDays() {
        assertThat(scheduler.retrievability(10, 10)).isCloseTo(0.9, within(EPS));
        assertThat(scheduler.retrievability(0, 10)).isCloseTo(1.0, within(EPS));
        assertThat(scheduler.retrievability(100, 10)).isLessThan(0.9);
    }

    @Test
    void intervalEqualsStabilityAtNinetyPercentRetentionAndIsCapped() {
        assertThat(scheduler.nextIntervalDays(46.3)).isEqualTo(46);
        assertThat(scheduler.nextIntervalDays(0.2)).isEqualTo(1);
        assertThat(scheduler.nextIntervalDays(1_000_000)).isEqualTo(36_500);
    }

    /** Replays reviews like the py-fsrs script: each review happens when the previous one made the card due. */
    private class Replay {

        UserCard card = UserCard.newCard(1L, 2L, START);
        Instant now = START;

        Replay review(Grade grade, CardState state, int step, double stability, double difficulty, Duration interval) {
            card = scheduler.review(card, grade, now);
            assertThat(card.state()).isEqualTo(state);
            assertThat(card.step()).isEqualTo(step);
            assertThat(card.stability()).isCloseTo(stability, within(EPS));
            assertThat(card.difficulty()).isCloseTo(difficulty, within(EPS));
            assertThat(Duration.between(now, card.dueAt())).isEqualTo(interval);
            now = card.dueAt();
            return this;
        }

        /** Reviews, then waits {@code wait} instead of until the due time before the next review. */
        Replay reviewAfter(Duration wait, Grade grade, CardState state, int step, double stability,
                           double difficulty, Duration interval) {
            Instant reviewedAt = now;
            review(grade, state, step, stability, difficulty, interval);
            now = reviewedAt.plus(wait);
            return this;
        }
    }
}
