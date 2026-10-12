package dev.words.application.review;

import dev.words.domain.Lexeme;
import dev.words.domain.UserCard;

/**
 * A card to review together with its word.
 */
public record DueCard(UserCard card, Lexeme lexeme) {
}
