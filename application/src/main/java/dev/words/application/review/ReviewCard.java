package dev.words.application.review;

import dev.words.application.error.NotFoundException;
import dev.words.application.port.LearningProfileRepository;
import dev.words.application.port.ReviewLogRepository;
import dev.words.application.port.UserCardRepository;
import dev.words.domain.FsrsScheduler;
import dev.words.domain.Grade;
import dev.words.domain.ReviewLog;
import dev.words.domain.UserCard;

import java.time.Clock;
import java.util.Objects;

/**
 * Records how well the user recalled a card and schedules its next review with FSRS.
 */
public class ReviewCard {

    private final LearningProfileRepository profiles;
    private final UserCardRepository cards;
    private final ReviewLogRepository reviewLogs;
    private final FsrsScheduler scheduler;
    private final Clock clock;

    public ReviewCard(LearningProfileRepository profiles, UserCardRepository cards, ReviewLogRepository reviewLogs,
                      FsrsScheduler scheduler, Clock clock) {
        this.profiles = profiles;
        this.cards = cards;
        this.reviewLogs = reviewLogs;
        this.scheduler = scheduler;
        this.clock = clock;
    }

    /**
     * @return the card with its new schedule
     * @throws NotFoundException if the card does not exist or belongs to another user
     */
    public UserCard review(long userId, long cardId, Grade grade) {
        Objects.requireNonNull(grade, "grade");
        var card = cards.findById(cardId)
                .filter(found -> belongsTo(found, userId))
                .orElseThrow(() -> new NotFoundException("Card " + cardId + " not found"));

        var now = clock.instant();
        var reviewed = cards.save(scheduler.review(card, grade, now));
        reviewLogs.save(new ReviewLog(null, reviewed.id(), grade, now));
        return reviewed;
    }

    private boolean belongsTo(UserCard card, long userId) {
        return profiles.findById(card.profileId())
                .map(profile -> profile.userId() == userId)
                .orElse(false);
    }
}
