package dev.words.bot;

import dev.words.application.word.AddedWord;
import dev.words.domain.CefrLevel;
import dev.words.domain.Example;
import dev.words.domain.IpaSource;
import dev.words.domain.Language;
import dev.words.domain.LearningProfile;
import dev.words.domain.Lexeme;
import dev.words.domain.UserCard;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class CardFormatterTest {

    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");
    private static final LearningProfile PROFILE =
            new LearningProfile(3L, 7L, Language.ENGLISH, Language.RUSSIAN, CefrLevel.A2, null, NOW);

    @Test
    void rendersWordIpaMeaningAndThreeExamples() {
        var lexeme = new Lexeme(1L, Language.ENGLISH, "borrow", "borrow", "verb", "ˈbɒɹəʊ", IpaSource.WIKTIONARY,
                Map.of(Language.RUSSIAN, "брать взаймы"));
        var examples = IntStream.range(1, 6)
                .mapToObj(i -> new Example((long) i, 1L, "Sentence " + i + ".", CefrLevel.A1,
                        Map.of(Language.RUSSIAN, "Предложение " + i + ".")))
                .toList();

        String card = CardFormatter.card(added(lexeme, examples, false));

        assertThat(card).isEqualTo("""
                <b>borrow</b>  /ˈbɒɹəʊ/  · <i>verb</i>
                брать взаймы

                1. Sentence 1.
                    <i>Предложение 1.</i>
                2. Sentence 2.
                    <i>Предложение 2.</i>
                3. Sentence 3.
                    <i>Предложение 3.</i>""");
    }

    @Test
    void skipsMissingPartsAndMarksWordAlreadyInDeck() {
        var lexeme = new Lexeme(1L, Language.ENGLISH, "look up", "look up", null, null, null, Map.of());

        assertThat(CardFormatter.card(added(lexeme, List.of(), true))).isEqualTo("""
                <b>look up</b>

                <i>Это слово уже в твоей колоде.</i>""");
    }

    @Test
    void escapesHtml() {
        var lexeme = new Lexeme(1L, Language.ENGLISH, "R&D <x>", "r&d <x>", null, null, null,
                Map.of(Language.RUSSIAN, "НИОКР & <b>"));

        assertThat(CardFormatter.card(added(lexeme, List.of(), false)))
                .isEqualTo("<b>R&amp;D &lt;x&gt;</b>\nНИОКР &amp; &lt;b&gt;");
    }

    private static AddedWord added(Lexeme lexeme, List<Example> examples, boolean alreadyInDeck) {
        return new AddedWord(PROFILE, lexeme, examples, UserCard.newCard(3L, 1L, NOW), alreadyInDeck);
    }
}
