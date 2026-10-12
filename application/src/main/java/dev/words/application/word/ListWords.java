package dev.words.application.word;

import dev.words.application.error.NoActiveProfileException;
import dev.words.application.error.NotFoundException;
import dev.words.application.port.LearningProfileRepository;
import dev.words.application.port.LexemeRepository;
import dev.words.application.port.UserCardRepository;
import dev.words.application.port.UserRepository;
import dev.words.domain.LearningProfile;
import dev.words.domain.Lexeme;
import dev.words.domain.UserCard;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Lists the words in the deck of the user's active profile, newest first, page by page.
 */
public class ListWords {

    private final UserRepository users;
    private final LearningProfileRepository profiles;
    private final UserCardRepository cards;
    private final LexemeRepository lexemes;

    public ListWords(UserRepository users, LearningProfileRepository profiles, UserCardRepository cards,
                     LexemeRepository lexemes) {
        this.users = users;
        this.profiles = profiles;
        this.cards = cards;
        this.lexemes = lexemes;
    }

    /**
     * @param page zero-based; a page past the end returns the last page
     * @throws NoActiveProfileException if the user has not chosen a language yet
     */
    public WordsPage list(long userId, int page, int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be positive: " + pageSize);
        }
        var profile = activeProfile(userId);
        long total = cards.countByProfileId(profile.id());
        int lastPage = (int) Math.max(0, (total - 1) / pageSize);
        int current = Math.clamp(page, 0, lastPage);

        var pageCards = cards.findByProfileId(profile.id(), current * pageSize, pageSize);
        Map<Long, Lexeme> byId = lexemes.findAllById(pageCards.stream().map(UserCard::lexemeId).toList()).stream()
                .collect(Collectors.toMap(Lexeme::id, Function.identity()));
        var words = pageCards.stream()
                .map(card -> byId.get(card.lexemeId()))
                .filter(lexeme -> lexeme != null)
                .toList();
        return new WordsPage(profile, words, current, pageSize, total);
    }

    private LearningProfile activeProfile(long userId) {
        var user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " not found"));
        if (!user.hasActiveProfile()) {
            throw new NoActiveProfileException(userId);
        }
        return profiles.findById(user.activeProfileId())
                .orElseThrow(() -> new NoActiveProfileException(userId));
    }
}
