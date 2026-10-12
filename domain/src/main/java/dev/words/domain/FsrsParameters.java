package dev.words.domain;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Settings of the FSRS-6 scheduler.
 *
 * @param weights          the 21 model weights w0..w20
 * @param desiredRetention probability of recalling a card when it becomes due
 * @param learningSteps    short intervals for a new card before it is scheduled in days
 * @param relearningSteps  short intervals after a forgotten card
 * @param maximumIntervalDays upper bound for an interval
 */
public record FsrsParameters(
        List<Double> weights,
        double desiredRetention,
        List<Duration> learningSteps,
        List<Duration> relearningSteps,
        int maximumIntervalDays) {

    /** Default FSRS-6 weights, as in the reference implementation py-fsrs 6. */
    public static final List<Double> DEFAULT_WEIGHTS = List.of(
            0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 0.001, 1.8722, 0.1666, 0.796,
            1.4835, 0.0614, 0.2629, 1.6483, 0.6014, 1.8729, 0.5425, 0.0912, 0.0658, 0.1542);

    public static final FsrsParameters DEFAULT = new FsrsParameters(
            DEFAULT_WEIGHTS,
            0.9,
            List.of(Duration.ofMinutes(1), Duration.ofMinutes(10)),
            List.of(Duration.ofMinutes(10)),
            36_500);

    public FsrsParameters {
        weights = List.copyOf(Objects.requireNonNull(weights, "weights"));
        learningSteps = List.copyOf(Objects.requireNonNull(learningSteps, "learningSteps"));
        relearningSteps = List.copyOf(Objects.requireNonNull(relearningSteps, "relearningSteps"));
        if (weights.size() != 21) {
            throw new IllegalArgumentException("FSRS-6 needs 21 weights, got " + weights.size());
        }
        if (desiredRetention <= 0 || desiredRetention >= 1) {
            throw new IllegalArgumentException("Desired retention must be between 0 and 1: " + desiredRetention);
        }
        if (maximumIntervalDays < 1) {
            throw new IllegalArgumentException("Maximum interval must be at least one day");
        }
    }
}
