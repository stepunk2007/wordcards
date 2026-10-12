package dev.words.infra.pronunciation;

import dev.words.domain.WordNormalizer;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.json.JsonFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Extracts one IPA transcription per word from a kaikki.org Wiktionary dump (JSON Lines, one entry per line).
 * <p>
 * A word has several entries (one per part of speech) and each lists several pronunciations. We keep the best one:
 * British (Received Pronunciation) first, since it is what Russian schools teach, then American, then untagged.
 * Phonemic {@code /.../} transcriptions beat phonetic {@code [...]} ones.
 */
class KaikkiIpaReader {

    private static final Set<String> BRITISH = Set.of("Received-Pronunciation", "UK", "British");
    private static final Set<String> AMERICAN = Set.of("General-American", "US", "American");

    private final JsonFactory jsonFactory = new JsonFactory();

    /**
     * @param langCode only entries of this language are read, e.g. "en"
     * @return IPA by normalized word, without the surrounding slashes
     */
    Map<String, String> read(InputStream dump, String langCode) throws IOException {
        Map<String, Candidate> best = new HashMap<>();
        var reader = new BufferedReader(new InputStreamReader(dump, StandardCharsets.UTF_8), 1 << 16);
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }
            var entry = parseEntry(line);
            if (entry == null || !langCode.equals(entry.langCode) || entry.word == null) {
                continue;
            }
            String key;
            try {
                key = WordNormalizer.normalize(entry.word);
            } catch (IllegalArgumentException e) {
                continue;
            }
            for (var candidate : entry.candidates) {
                best.merge(key, candidate, (current, offered) -> offered.rank < current.rank ? offered : current);
            }
        }
        Map<String, String> result = new HashMap<>(best.size());
        best.forEach((word, candidate) -> result.put(word, candidate.ipa));
        return result;
    }

    private Entry parseEntry(String line) {
        var entry = new Entry();
        try (JsonParser parser = jsonFactory.createParser(tools.jackson.core.ObjectReadContext.empty(), line)) {
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                return null;
            }
            while (parser.nextToken() == JsonToken.PROPERTY_NAME) {
                String field = parser.currentName();
                JsonToken value = parser.nextToken();
                switch (field) {
                    case "word" -> entry.word = value == JsonToken.VALUE_STRING ? parser.getString() : null;
                    case "lang_code" -> entry.langCode = value == JsonToken.VALUE_STRING ? parser.getString() : null;
                    case "sounds" -> readSounds(parser, value, entry.candidates);
                    default -> parser.skipChildren();
                }
            }
            return entry;
        } catch (RuntimeException e) {
            // A malformed line must not stop a multi-gigabyte import.
            return null;
        }
    }

    private static void readSounds(JsonParser parser, JsonToken value, List<Candidate> candidates) {
        if (value != JsonToken.START_ARRAY) {
            parser.skipChildren();
            return;
        }
        while (parser.nextToken() == JsonToken.START_OBJECT) {
            String ipa = null;
            List<String> tags = new ArrayList<>();
            while (parser.nextToken() == JsonToken.PROPERTY_NAME) {
                String field = parser.currentName();
                JsonToken token = parser.nextToken();
                if ("ipa".equals(field) && token == JsonToken.VALUE_STRING) {
                    ipa = parser.getString();
                } else if ("tags".equals(field) && token == JsonToken.START_ARRAY) {
                    while (parser.nextToken() == JsonToken.VALUE_STRING) {
                        tags.add(parser.getString());
                    }
                } else {
                    parser.skipChildren();
                }
            }
            var candidate = Candidate.of(ipa, tags);
            if (candidate != null) {
                candidates.add(candidate);
            }
        }
    }

    private static final class Entry {
        String word;
        String langCode;
        final List<Candidate> candidates = new ArrayList<>();
    }

    private record Candidate(String ipa, int rank) {

        static Candidate of(String raw, List<String> tags) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            String ipa = raw.strip();
            boolean phonetic = ipa.startsWith("[");
            ipa = stripDelimiters(ipa);
            if (ipa.isEmpty() || ipa.length() > 200) {
                return null;
            }
            int accent;
            if (tags.stream().anyMatch(BRITISH::contains)) {
                accent = 0;
            } else if (tags.stream().anyMatch(AMERICAN::contains)) {
                accent = 1;
            } else if (tags.isEmpty()) {
                accent = 2;
            } else {
                accent = 3;
            }
            return new Candidate(ipa, accent + (phonetic ? 10 : 0));
        }

        private static String stripDelimiters(String ipa) {
            if (ipa.length() >= 2 && (ipa.startsWith("/") && ipa.endsWith("/")
                    || ipa.startsWith("[") && ipa.endsWith("]"))) {
                return ipa.substring(1, ipa.length() - 1).strip();
            }
            return ipa;
        }
    }
}
