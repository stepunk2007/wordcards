package dev.words.application.review;

import dev.words.domain.LearningProfile;

import java.util.List;

/**
 * Cards of the active profile that are due now, most overdue first.
 *
 * @param totalDue all due cards, possibly more than returned
 */
public record DueCards(LearningProfile profile, List<DueCard> cards, long totalDue) {

    public DueCards {
        cards = List.copyOf(cards);
    }
}
