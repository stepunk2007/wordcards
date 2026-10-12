package dev.words.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CefrLevelTest {

    @Test
    void parsesLevelIgnoringCase() {
        assertThat(CefrLevel.parse("b1")).isEqualTo(CefrLevel.B1);
        assertThat(CefrLevel.parse(" C2 ")).isEqualTo(CefrLevel.C2);
    }

    @Test
    void levelsAreOrderedFromBeginnerToMastery() {
        assertThat(CefrLevel.A1).isLessThan(CefrLevel.B2);
        assertThat(CefrLevel.values()).hasSize(6);
    }

    @Test
    void rejectsUnknownLevel() {
        assertThatThrownBy(() -> CefrLevel.parse("D1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CefrLevel.parse(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
