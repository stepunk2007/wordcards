package dev.words.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * FSRS-6 spaced repetition: after each review computes the card's new stability, difficulty and due time.
 * <p>
 * Follows the reference implementation py-fsrs 6 without interval fuzzing, so results are deterministic.
 * Stability is in days: the interval after which the chance of recalling the card falls to 90%.
 * Difficulty goes from 1 (easy) to 10 (hard).
 */
public class FsrsScheduler {

    private static final double STABILITY_MIN = 0.001;
    private static final double DIFFICULTY_MIN = 1.0;
    private static final double DIFFICULTY_MAX = 10.0;

    private final FsrsParameters parameters;
    private final double[] w;
    private final double decay;
    private final double factor;

    public FsrsScheduler(FsrsParameters parameters) {
        this.parameters = parameters;
        this.w = parameters.weights().stream().mapToDouble(Double::doubleValue).toArray();
        this.decay = -w[20];
        this.factor = Math.pow(0.9, 1 / decay) - 1;
    }

    public FsrsScheduler() {
        this(FsrsParameters.DEFAULT);
    }

    public UserCard review(UserCard card, Grade grade, Instant now) {
        double stability;
        double difficulty;
        if (card.state() == CardState.NEW || card.lastReviewedAt() == null) {
            stability = initialStability(grade);
            difficulty = initialDifficulty(grade, true);
        } else {
            long elapsedDays = Math.max(0, Duration.between(card.lastReviewedAt(), now).toDays());
            stability = elapsedDays < 1
                    ? shortTermStability(card.stability(), grade)
                    : nextStability(card.difficulty(), card.stability(),
                            retrievability(elapsedDays, card.stability()), grade);
            difficulty = nextDifficulty(card.difficulty(), grade);
        }

        CardState state;
        int step = 0;
        Duration interval;
        int lapses = card.lapses();
        switch (card.state()) {
            case NEW, LEARNING, RELEARNING -> {
                var steps = card.state() == CardState.RELEARNING
                        ? parameters.relearningSteps()
                        : parameters.learningSteps();
                var learningState = card.state() == CardState.RELEARNING ? CardState.RELEARNING : CardState.LEARNING;
                int current = card.state() == CardState.NEW ? 0 : card.step();
                if (steps.isEmpty() || (current >= steps.size() && grade != Grade.AGAIN)) {
                    state = CardState.REVIEW;
                    interval = days(nextIntervalDays(stability));
                } else {
                    switch (grade) {
                        case AGAIN -> {
                            state = learningState;
                            interval = steps.getFirst();
                        }
                        case HARD -> {
                            state = learningState;
                            step = current;
                            interval = hardStep(steps, current);
                        }
                        case GOOD -> {
                            if (current + 1 == steps.size()) {
                                state = CardState.REVIEW;
                                interval = days(nextIntervalDays(stability));
                            } else {
                                state = learningState;
                                step = current + 1;
                                interval = steps.get(step);
                            }
                        }
                        case EASY -> {
                            state = CardState.REVIEW;
                            interval = days(nextIntervalDays(stability));
                        }
                        default -> throw new IllegalStateException();
                    }
                }
            }
            case REVIEW -> {
                if (grade == Grade.AGAIN) {
                    lapses++;
                    if (parameters.relearningSteps().isEmpty()) {
                        state = CardState.REVIEW;
                        interval = days(nextIntervalDays(stability));
                    } else {
                        state = CardState.RELEARNING;
                        interval = parameters.relearningSteps().getFirst();
                    }
                } else {
                    state = CardState.REVIEW;
                    interval = days(nextIntervalDays(stability));
                }
            }
            default -> throw new IllegalStateException("Unknown card state " + card.state());
        }

        return new UserCard(card.id(), card.profileId(), card.lexemeId(), state, now.plus(interval),
                stability, difficulty, card.reps() + 1, lapses, step, now, card.createdAt());
    }

    /**
     * Probability of recalling a card with this stability after {@code elapsedDays}.
     */
    public double retrievability(double elapsedDays, double stability) {
        return Math.pow(1 + factor * Math.max(0, elapsedDays) / stability, decay);
    }

    int nextIntervalDays(double stability) {
        double interval = stability / factor * (Math.pow(parameters.desiredRetention(), 1 / decay) - 1);
        return (int) Math.min(Math.max(1, Math.round(interval)), parameters.maximumIntervalDays());
    }

    private static Duration hardStep(List<Duration> steps, int current) {
        if (current == 0 && steps.size() == 1) {
            return Duration.ofMillis(Math.round(steps.getFirst().toMillis() * 1.5));
        }
        if (current == 0) {
            return steps.get(0).plus(steps.get(1)).dividedBy(2);
        }
        return steps.get(current);
    }

    private double initialStability(Grade grade) {
        return clampStability(w[grade.value() - 1]);
    }

    private double initialDifficulty(Grade grade, boolean clamp) {
        double difficulty = w[4] - Math.exp(w[5] * (grade.value() - 1)) + 1;
        return clamp ? clampDifficulty(difficulty) : difficulty;
    }

    private double shortTermStability(double stability, Grade grade) {
        double increase = Math.exp(w[17] * (grade.value() - 3 + w[18])) * Math.pow(stability, -w[19]);
        if (grade != Grade.AGAIN) {
            increase = Math.max(increase, 1.0);
        }
        return clampStability(stability * increase);
    }

    private double nextDifficulty(double difficulty, Grade grade) {
        double delta = -w[6] * (grade.value() - 3);
        double damped = difficulty + delta * (10 - difficulty) / 9;
        double reverted = w[7] * initialDifficulty(Grade.EASY, false) + (1 - w[7]) * damped;
        return clampDifficulty(reverted);
    }

    private double nextStability(double difficulty, double stability, double retrievability, Grade grade) {
        return clampStability(grade == Grade.AGAIN
                ? forgetStability(difficulty, stability, retrievability)
                : recallStability(difficulty, stability, retrievability, grade));
    }

    private double forgetStability(double difficulty, double stability, double retrievability) {
        double longTerm = w[11] * Math.pow(difficulty, -w[12]) * (Math.pow(stability + 1, w[13]) - 1)
                * Math.exp((1 - retrievability) * w[14]);
        double shortTerm = stability / Math.exp(w[17] * w[18]);
        return Math.min(longTerm, shortTerm);
    }

    private double recallStability(double difficulty, double stability, double retrievability, Grade grade) {
        double hardPenalty = grade == Grade.HARD ? w[15] : 1;
        double easyBonus = grade == Grade.EASY ? w[16] : 1;
        return stability * (1 + Math.exp(w[8]) * (11 - difficulty) * Math.pow(stability, -w[9])
                * (Math.exp((1 - retrievability) * w[10]) - 1) * hardPenalty * easyBonus);
    }

    private static Duration days(int days) {
        return Duration.ofDays(days);
    }

    private static double clampStability(double stability) {
        return Math.max(stability, STABILITY_MIN);
    }

    private static double clampDifficulty(double difficulty) {
        return Math.min(Math.max(difficulty, DIFFICULTY_MIN), DIFFICULTY_MAX);
    }
}
