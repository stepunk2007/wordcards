package dev.words.domain;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExampleTest {

    @Test
    void returnsTranslationForNativeLanguage() {
        var example = new Example(null, 1L, "Can I borrow your pen?", CefrLevel.A1,
                Map.of(Language.RUSSIAN, "Можно взять твою ручку?"));

        assertThat(example.translationIn(Language.RUSSIAN)).contains("Можно взять твою ручку?");
        assertThat(example.translationIn(Language.ENGLISH)).isEmpty();
    }

    @Test
    void rejectsBlankText() {
        assertThatThrownBy(() -> new Example(null, 1L, " ", CefrLevel.A1, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
