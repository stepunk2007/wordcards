package dev.words.domain;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Turns user input into the key used to look up a shared lexeme,
 * so that "Don’t  Worry", "don't worry" and " DON'T WORRY " find the same card.
 */
public final class WordNormalizer {

    private static final Pattern APOSTROPHES = Pattern.compile("[‘’ʼʹ`´′]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private WordNormalizer() {
    }

    public static String normalize(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Word text is required");
        }
        // Apostrophes first: NFKC would decompose the acute accent into a space and a combining mark.
        String normalized = APOSTROPHES.matcher(text).replaceAll("'");
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFKC);
        normalized = WHITESPACE.matcher(normalized.strip()).replaceAll(" ");
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Word text is blank");
        }
        return normalized;
    }
}
