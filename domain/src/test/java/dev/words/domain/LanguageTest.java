package dev.words.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LanguageTest {

    @Test
    void parsesIsoCodeIgnoringCaseAndSpaces() {
        assertThat(Language.fromCode("en")).isEqualTo(Language.ENGLISH);
        assertThat(Language.fromCode(" RU ")).isEqualTo(Language.RUSSIAN);
    }

    @Test
    void exposesIsoCode() {
        assertThat(Language.ENGLISH.code()).isEqualTo("en");
    }

    @Test
    void rejectsUnknownCode() {
        assertThatThrownBy(() -> Language.fromCode("xx")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Language.fromCode(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
