package dev.words.application.port;

import dev.words.domain.CefrLevel;
import dev.words.domain.Language;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmPortContractTest {

    @Test
    void requestRejectsBlankWord() {
        assertThatThrownBy(() -> new WordCardRequest(" ", Language.ENGLISH, Language.RUSSIAN, CefrLevel.B1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cardKeepsExamplesImmutable() {
        var examples = new ArrayList<>(List.of(
                new GeneratedExample("Can I borrow your pen?", "Можно взять твою ручку?", CefrLevel.A1)));

        var card = new GeneratedWordCard("borrow", "verb", " ˈbɒrəʊ ", "брать взаймы", examples);
        examples.clear();

        assertThat(card.examples()).hasSize(1);
        assertThatThrownBy(() -> card.examples().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void ipaIsTrimmedAndBlankIpaBecomesNull() {
        assertThat(new GeneratedWordCard("borrow", "verb", " ˈbɒrəʊ ", "x", List.of()).ipa()).isEqualTo("ˈbɒrəʊ");
        assertThat(new GeneratedWordCard("borrow", "verb", " ", "x", List.of()).ipa()).isNull();
    }

    @Test
    void cardWithoutExamplesHasEmptyList() {
        assertThat(new GeneratedWordCard("borrow", null, " ", "брать взаймы", null).examples()).isEmpty();
    }

    @Test
    void rejectsBlankLemmaAndExample() {
        assertThatThrownBy(() -> new GeneratedWordCard(" ", "verb", null, "x", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GeneratedExample("", "x", CefrLevel.A1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
