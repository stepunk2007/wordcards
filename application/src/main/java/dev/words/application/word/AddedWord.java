package dev.words.application.word;

import dev.words.domain.Example;
import dev.words.domain.LearningProfile;
import dev.words.domain.Lexeme;
import dev.words.domain.UserCard;

import java.util.List;

/**
 * Result of adding a word: everything needed to show the card.
 *
 * @param alreadyInDeck true if the word was in the deck before, nothing new was added
 */
public record AddedWord(
        LearningProfile profile,
        Lexeme lexeme,
        List<Example> examples,
        UserCard card,
        boolean alreadyInDeck) {

    public AddedWord {
        examples = List.copyOf(examples);
    }
}
