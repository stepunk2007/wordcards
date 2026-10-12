package dev.words;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class LexemeSchemaTest extends PostgresIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void storesLexemeWithMeaningExampleAndTranslation() {
        long lexemeId = insertLexeme("en", "Borrow", "borrow");
        jdbc.update("insert into lexeme_meaning (lexeme_id, native_lang, meaning) values (?, 'ru', 'брать взаймы')",
                lexemeId);
        long exampleId = insertExample(lexemeId, "Can I borrow your pen?", "A1");
        jdbc.update("insert into example_translation (example_id, native_lang, translation) values (?, 'ru', ?)",
                exampleId, "Можно взять твою ручку?");

        String translation = jdbc.queryForObject("""
                select t.translation
                from lexeme l
                join example e on e.lexeme_id = l.id
                join example_translation t on t.example_id = e.id and t.native_lang = 'ru'
                where l.lang = 'en' and l.normalized_text = 'borrow'
                """, String.class);
        assertThat(translation).isEqualTo("Можно взять твою ручку?");
    }

    @Test
    void lexemeIsUniquePerLanguageAndNormalizedText() {
        insertLexeme("en", "Borrow", "borrow");

        assertThatThrownBy(() -> insertLexeme("en", "borrow", "borrow"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void sameTextInAnotherLanguageIsAnotherLexeme() {
        insertLexeme("en", "son", "son");

        assertThat(insertLexeme("es", "son", "son")).isPositive();
    }

    @Test
    void ipaRequiresSource() {
        assertThatThrownBy(() -> jdbc.update(
                "insert into lexeme (lang, text, normalized_text, ipa) values ('en', 'borrow', 'borrow', 'ˈbɒrəʊ')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void oneMeaningPerNativeLanguage() {
        long lexemeId = insertLexeme("en", "borrow", "borrow");
        jdbc.update("insert into lexeme_meaning (lexeme_id, native_lang, meaning) values (?, 'ru', 'брать')", lexemeId);

        assertThatThrownBy(() -> jdbc.update(
                "insert into lexeme_meaning (lexeme_id, native_lang, meaning) values (?, 'ru', 'занять')", lexemeId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingLexemeDeletesItsExamplesAndTranslations() {
        long lexemeId = insertLexeme("en", "borrow", "borrow");
        long exampleId = insertExample(lexemeId, "Can I borrow your pen?", "A1");
        jdbc.update("insert into example_translation (example_id, native_lang, translation) values (?, 'ru', 'x')",
                exampleId);

        jdbc.update("delete from lexeme where id = ?", lexemeId);

        assertThat(jdbc.queryForObject("select count(*) from example_translation where example_id = ?",
                Integer.class, exampleId)).isZero();
    }

    @Test
    void rejectsBlankExampleText() {
        long lexemeId = insertLexeme("en", "borrow", "borrow");

        assertThatThrownBy(() -> insertExample(lexemeId, "  ", "A1"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private long insertLexeme(String lang, String text, String normalizedText) {
        return jdbc.queryForObject(
                "insert into lexeme (lang, text, normalized_text) values (?, ?, ?) returning id",
                Long.class, lang, text, normalizedText);
    }

    private long insertExample(long lexemeId, String text, String level) {
        return jdbc.queryForObject(
                "insert into example (lexeme_id, text, level) values (?, ?, ?) returning id",
                Long.class, lexemeId, text, level);
    }
}
