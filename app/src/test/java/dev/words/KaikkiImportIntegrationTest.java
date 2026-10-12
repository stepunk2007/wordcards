package dev.words;

import dev.words.domain.Language;
import dev.words.infra.pronunciation.KaikkiImporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class KaikkiImportIntegrationTest extends PostgresIntegrationTest {

    @TempDir
    Path dir;

    @Autowired
    KaikkiImporter importer;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void importsGzippedDumpIdempotently() throws Exception {
        Path dump = dir.resolve("dump.jsonl.gz");
        try (var out = new GZIPOutputStream(Files.newOutputStream(dump))) {
            out.write("""
                    {"word": "borrow", "lang_code": "en", "sounds": [{"ipa": "/ˈbɒɹəʊ/", "tags": ["UK"]}]}
                    {"word": "Lend", "lang_code": "en", "sounds": [{"ipa": "/lɛnd/"}]}
                    """.getBytes(StandardCharsets.UTF_8));
        }

        assertThat(importer.importDump(dump.toString(), Language.ENGLISH)).isEqualTo(2);
        importer.importDump(dump.toString(), Language.ENGLISH);

        assertThat(jdbc.queryForList(
                "select normalized_text || ' ' || ipa || ' ' || source from pronunciation where lang = 'en' order by 1",
                String.class))
                .containsExactly("borrow ˈbɒɹəʊ WIKTIONARY", "lend lɛnd WIKTIONARY");
    }
}
