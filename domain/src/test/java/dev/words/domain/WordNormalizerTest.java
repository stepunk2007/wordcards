package dev.words.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WordNormalizerTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Borrow|borrow",
            "  borrow  |borrow",
            "LOOK   UP|look up",
            "don’t|don't",
            "don‘t|don't",
            "donʼt|don't",
            "don`t|don't",
            "don´t|don't",
            "Ｂｏｒｒｏｗ|borrow",
            "Café|café",
            "Ёлка|ёлка",
    })
    void normalizesCaseSpacesAndApostrophes(String input, String expected) {
        assertThat(WordNormalizer.normalize(input)).isEqualTo(expected);
    }

    @Test
    void collapsesTabsAndNewlines() {
        assertThat(WordNormalizer.normalize("look\tafter\n it")).isEqualTo("look after it");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t\n"})
    void rejectsBlankText(String input) {
        assertThatThrownBy(() -> WordNormalizer.normalize(input)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> WordNormalizer.normalize(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
