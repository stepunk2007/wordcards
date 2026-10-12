package dev.words.application.port;

import dev.words.domain.Language;

/**
 * The text the user sent is not a word or phrase of the language being learned:
 * another language, a typo beyond recognition or random characters.
 */
public class WordNotInLanguageException extends RuntimeException {

    private final String text;
    private final Language expectedLanguage;

    public WordNotInLanguageException(String text, Language expectedLanguage) {
        super("'" + text + "' is not a " + expectedLanguage + " word");
        this.text = text;
        this.expectedLanguage = expectedLanguage;
    }

    public String text() {
        return text;
    }

    public Language expectedLanguage() {
        return expectedLanguage;
    }
}
