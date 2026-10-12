package dev.words.domain;

import java.util.Locale;

/**
 * Language proficiency level on the CEFR scale, from beginner (A1) to mastery (C2).
 */
public enum CefrLevel {
    A1, A2, B1, B2, C1, C2;

    public static CefrLevel parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("CEFR level is required");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown CEFR level: " + value, e);
        }
    }
}
