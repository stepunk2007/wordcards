package dev.words.application.port;

import dev.words.domain.IpaSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PronunciationTest {

    @Test
    void keepsIpaWithoutSurroundingSpaces() {
        var pronunciation = new Pronunciation(" ˈbɒrəʊ ", IpaSource.WIKTIONARY);

        assertThat(pronunciation.ipa()).isEqualTo("ˈbɒrəʊ");
        assertThat(pronunciation.source()).isEqualTo(IpaSource.WIKTIONARY);
    }

    @Test
    void rejectsBlankIpaOrMissingSource() {
        assertThatThrownBy(() -> new Pronunciation(" ", IpaSource.LLM)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Pronunciation("ˈbɒrəʊ", null)).isInstanceOf(NullPointerException.class);
    }
}
