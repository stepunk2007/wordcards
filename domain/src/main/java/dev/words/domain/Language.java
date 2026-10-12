package dev.words.domain;

import java.util.Arrays;
import java.util.Locale;

/**
 * A language identified by its ISO 639-1 code.
 */
public enum Language {
    ENGLISH("en"),
    RUSSIAN("ru");

    private final String code;

    Language(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static Language fromCode(String code) {
        String normalized = code == null ? "" : code.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(language -> language.code.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported language code: " + code));
    }
}
