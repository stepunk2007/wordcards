package dev.words.domain;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LexemeTest {

    @Test
    void createKeepsOriginalTextAndComputesNormalizedKey() {
        var lexeme = Lexeme.create(Language.ENGLISH, "  Don’t Worry ", "phrase", null, null,
                Map.of(Language.RUSSIAN, "не волнуйся"));

        assertThat(lexeme.text()).isEqualTo("Don’t Worry");
        assertThat(lexeme.normalizedText()).isEqualTo("don't worry");
        assertThat(lexeme.meaningIn(Language.RUSSIAN)).contains("не волнуйся");
        assertThat(lexeme.meaningIn(Language.ENGLISH)).isEmpty();
    }

    @Test
    void requiresIpaAndSourceTogether() {
        assertThatThrownBy(() -> Lexeme.create(Language.ENGLISH, "borrow", "verb", "ˈbɒrəʊ", null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Lexeme.create(Language.ENGLISH, "borrow", "verb", null, IpaSource.LLM, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void meaningsAreImmutable() {
        var lexeme = Lexeme.create(Language.ENGLISH, "borrow", "verb", "ˈbɒrəʊ", IpaSource.WIKTIONARY, null);

        assertThat(lexeme.meanings()).isEmpty();
        assertThatThrownBy(() -> lexeme.meanings().put(Language.RUSSIAN, "x"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
