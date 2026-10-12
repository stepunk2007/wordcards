package dev.words.infra.pronunciation;

import dev.words.domain.IpaSource;
import dev.words.domain.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Loads IPA from a kaikki.org dump into the {@code pronunciation} table.
 * The dump can be a local file or an http(s) URL, plain or gzipped; it is streamed, never stored.
 */
@Component
public class KaikkiImporter {

    private static final Logger log = LoggerFactory.getLogger(KaikkiImporter.class);
    private static final int BATCH_SIZE = 5_000;

    private final JdbcTemplate jdbc;
    private final KaikkiIpaReader reader = new KaikkiIpaReader();

    KaikkiImporter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * @return number of words stored
     */
    public int importDump(String location, Language language) throws IOException {
        log.info("Reading {} IPA from {}", language, location);
        long started = System.nanoTime();
        Map<String, String> ipaByWord;
        try (InputStream in = open(location)) {
            ipaByWord = reader.read(in, language.code());
        }
        log.info("Found IPA for {} words, saving", ipaByWord.size());

        List<Object[]> batch = new ArrayList<>(BATCH_SIZE);
        for (var entry : ipaByWord.entrySet()) {
            batch.add(new Object[] {language.code(), entry.getKey(), entry.getValue(), IpaSource.WIKTIONARY.name()});
            if (batch.size() == BATCH_SIZE) {
                save(batch);
                batch.clear();
            }
        }
        save(batch);

        log.info("Imported {} {} pronunciations in {} s", ipaByWord.size(), language,
                (System.nanoTime() - started) / 1_000_000_000);
        return ipaByWord.size();
    }

    private void save(List<Object[]> batch) {
        if (batch.isEmpty()) {
            return;
        }
        jdbc.batchUpdate("""
                insert into pronunciation (lang, normalized_text, ipa, source)
                values (?, ?, ?, ?)
                on conflict (lang, normalized_text) do update set ipa = excluded.ipa, source = excluded.source
                """, batch);
    }

    private static InputStream open(String location) throws IOException {
        InputStream raw = location.startsWith("http://") || location.startsWith("https://")
                ? URI.create(location).toURL().openStream()
                : Files.newInputStream(Path.of(location));
        InputStream buffered = new BufferedInputStream(raw, 1 << 16);
        return location.endsWith(".gz") ? new GZIPInputStream(buffered, 1 << 16) : buffered;
    }
}
