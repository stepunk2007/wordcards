package dev.words.infra.pronunciation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KaikkiIpaReaderTest {

    private Map<String, String> ipa;

    @BeforeEach
    void readSample() throws IOException {
        try (var dump = getClass().getResourceAsStream("/kaikki-sample.jsonl")) {
            ipa = new KaikkiIpaReader().read(dump, "en");
        }
    }

    @Test
    void prefersFirstBritishTranscriptionWithoutSlashes() {
        assertThat(ipa).containsEntry("dictionary", "ˈdɪk.ʃə.nə.ɹi");
    }

    @Test
    void britishFromAnotherEntryBeatsAmericanAndKeysAreNormalized() {
        assertThat(ipa).containsEntry("borrow", "ˈbɒɹəʊ");
    }

    @Test
    void phonemicBeatsPhonetic() {
        assertThat(ipa).containsEntry("tomato", "təˈmɑːtəʊ");
    }

    @Test
    void keepsPhrasesAndNormalizesApostrophes() {
        assertThat(ipa).containsEntry("look up", "lʊk ˈʌp").containsEntry("don't", "dəʊnt");
    }

    @Test
    void skipsOtherLanguagesWordsWithoutIpaAndBrokenLines() {
        assertThat(ipa).doesNotContainKeys("hund", "silent", "nosounds", "словарь");
        assertThat(ipa).hasSize(5);
    }
}
