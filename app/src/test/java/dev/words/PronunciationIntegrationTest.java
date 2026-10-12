package dev.words;

import dev.words.application.port.Pronunciation;
import dev.words.application.port.PronunciationPort;
import dev.words.application.word.PronunciationResolver;
import dev.words.domain.IpaSource;
import dev.words.domain.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class PronunciationIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    PronunciationPort pronunciations;

    @Autowired
    PronunciationResolver resolver;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.update("""
                insert into pronunciation (lang, normalized_text, ipa, source)
                values ('en', 'look up', 'ˌlʊk ˈʌp', 'WIKTIONARY'), ('en', 'don''t', 'dəʊnt', 'WIKTIONARY')
                on conflict do nothing
                """);
    }

    @Test
    void findsWordByNormalizedText() {
        assertThat(pronunciations.findIpa(Language.ENGLISH, "  Look   UP "))
                .contains(new Pronunciation("ˌlʊk ˈʌp", IpaSource.WIKTIONARY));
        assertThat(pronunciations.findIpa(Language.ENGLISH, "Don’t"))
                .contains(new Pronunciation("dəʊnt", IpaSource.WIKTIONARY));
    }

    @Test
    void unknownWordOrLanguageIsEmpty() {
        assertThat(pronunciations.findIpa(Language.ENGLISH, "qwertyzzz")).isEmpty();
        assertThat(pronunciations.findIpa(Language.RUSSIAN, "look up")).isEmpty();
        assertThat(pronunciations.findIpa(Language.ENGLISH, "  ")).isEmpty();
    }

    @Test
    void resolverFallsBackToLlmIpa() {
        assertThat(resolver.resolve(Language.ENGLISH, "look up", "looked up", "lʊk ʌp"))
                .contains(new Pronunciation("ˌlʊk ˈʌp", IpaSource.WIKTIONARY));
        assertThat(resolver.resolve(Language.ENGLISH, "qwertyzzz", "qwertyzzz", "ˈkwɜːti"))
                .contains(new Pronunciation("ˈkwɜːti", IpaSource.LLM));
    }
}
