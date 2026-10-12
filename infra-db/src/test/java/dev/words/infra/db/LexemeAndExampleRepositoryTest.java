package dev.words.infra.db;

import dev.words.application.port.ExampleRepository;
import dev.words.application.port.LexemeRepository;
import dev.words.domain.CefrLevel;
import dev.words.domain.Example;
import dev.words.domain.IpaSource;
import dev.words.domain.Language;
import dev.words.domain.Lexeme;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LexemeAndExampleRepositoryTest extends RepositoryTest {

    @Autowired
    LexemeRepository lexemes;

    @Autowired
    ExampleRepository examples;

    @Test
    void savesLexemeWithMeaningsAndFindsItByNormalizedText() {
        var saved = lexemes.save(Lexeme.create(Language.ENGLISH, "Borrow", "verb", "ˈbɒrəʊ",
                IpaSource.WIKTIONARY, Map.of(Language.RUSSIAN, "брать взаймы")));

        var found = lexemes.findByLanguageAndNormalizedText(Language.ENGLISH, "borrow").orElseThrow();

        assertThat(found).isEqualTo(saved);
        assertThat(found.text()).isEqualTo("Borrow");
        assertThat(found.meaningIn(Language.RUSSIAN)).contains("брать взаймы");
        assertThat(lexemes.findByLanguageAndNormalizedText(Language.RUSSIAN, "borrow")).isEmpty();
    }

    @Test
    void addsMeaningInAnotherLanguageToExistingLexeme() {
        var saved = lexemes.save(Lexeme.create(Language.RUSSIAN, "книга", "noun", null, null,
                Map.of()));

        var withMeaning = new Lexeme(saved.id(), saved.language(), saved.text(), saved.normalizedText(),
                saved.partOfSpeech(), "ˈknʲiɡə", IpaSource.LLM, Map.of(Language.ENGLISH, "book"));
        lexemes.save(withMeaning);

        assertThat(lexemes.findById(saved.id())).contains(withMeaning);
    }

    @Test
    void findsSeveralLexemesById() {
        var a = lexemes.save(Lexeme.create(Language.ENGLISH, "alpha", null, null, null, Map.of()));
        var b = lexemes.save(Lexeme.create(Language.ENGLISH, "beta", null, null, null, Map.of()));

        assertThat(lexemes.findAllById(List.of(a.id(), b.id(), 999_999L))).containsExactlyInAnyOrder(a, b);
    }

    @Test
    void duplicateLexemeIsRejected() {
        lexemes.save(Lexeme.create(Language.ENGLISH, "Look up", "phrasal verb", null, null, Map.of()));

        assertThatThrownBy(() -> lexemes.save(Lexeme.create(Language.ENGLISH, "look  UP", null, null, null,
                Map.of()))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void savesExamplesWithTranslationsInOrder() {
        var lexeme = lexemes.save(Lexeme.create(Language.ENGLISH, "borrow", "verb", null, null, Map.of()));

        var saved = examples.saveAll(List.of(
                new Example(null, lexeme.id(), "Can I borrow your pen?", CefrLevel.A1,
                        Map.of(Language.RUSSIAN, "Можно взять твою ручку?")),
                new Example(null, lexeme.id(), "She borrowed a book from the library.", CefrLevel.A2,
                        Map.of(Language.RUSSIAN, "Она взяла книгу в библиотеке."))));

        assertThat(saved).allSatisfy(example -> assertThat(example.id()).isNotNull());
        assertThat(examples.findByLexemeId(lexeme.id())).containsExactlyElementsOf(saved);
        assertThat(examples.findByLexemeId(lexeme.id()).getFirst().translationIn(Language.RUSSIAN))
                .contains("Можно взять твою ручку?");
    }
}
