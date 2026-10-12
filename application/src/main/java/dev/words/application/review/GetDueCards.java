package dev.words.application.review;

import dev.words.application.port.LearningProfileRepository;
import dev.words.application.port.LexemeRepository;
import dev.words.application.port.UserCardRepository;
import dev.words.application.port.UserRepository;
import dev.words.domain.Lexeme;
import dev.words.domain.UserCard;

import java.time.Clock;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cards of the active profile that are due for review now, most overdue first.
 */
public class GetDueCards {

    private final UserRepository users;
    private final LearningProfileRepository profiles;
    private final UserCardRepository cards;
    private final LexemeRepository lexemes;
    private final Clock clock;

    public GetDueCards(UserRepository users, LearningProfileRepository profiles, UserCardRepository cards,
                       LexemeRepository lexemes, Clock clock) {
        this.users = users;
        this.profiles = profiles;
        this.cards = cards;
        this.lexemes = lexemes;
        this.clock = clock;
    }

    /**
     * @param limit how many cards to return at most
     * @throws dev.words.application.error.NoActiveProfileException if the user has not chosen a language yet
     */
    public DueCards due(long userId, int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive: " + limit);
        }
        var profile = ActiveProfiles.of(userId, users, profiles);
        var now = clock.instant();

        var dueCards = cards.findDue(profile.id(), now, limit);
        Map<Long, Lexeme> byId = lexemes.findAllById(dueCards.stream().map(UserCard::lexemeId).toList()).stream()
                .collect(Collectors.toMap(Lexeme::id, Function.identity()));
        var result = dueCards.stream()
                .filter(card -> byId.containsKey(card.lexemeId()))
                .map(card -> new DueCard(card, byId.get(card.lexemeId())))
                .toList();
        return new DueCards(profile, result, cards.countDue(profile.id(), now));
    }
}
