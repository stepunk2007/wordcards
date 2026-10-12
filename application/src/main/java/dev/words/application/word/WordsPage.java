package dev.words.application.word;

import dev.words.domain.LearningProfile;
import dev.words.domain.Lexeme;

import java.util.List;

/**
 * One page of the words in the active profile's deck, newest first.
 *
 * @param page       zero-based page number, clamped to the existing pages
 * @param totalWords words in the deck
 */
public record WordsPage(LearningProfile profile, List<Lexeme> words, int page, int pageSize, long totalWords) {

    public WordsPage {
        words = List.copyOf(words);
    }

    public int totalPages() {
        return (int) Math.max(1, (totalWords + pageSize - 1) / pageSize);
    }

    public boolean hasPrevious() {
        return page > 0;
    }

    public boolean hasNext() {
        return page + 1 < totalPages();
    }
}
