package dev.words.infra.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.words.application.port.GeneratedExample;
import dev.words.domain.CefrLevel;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WordCardResponseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parsesSavedResponseIntoGeneratedCard() throws IOException {
        WordCardResponse response;
        try (var json = getClass().getResourceAsStream("/llm/borrow-response.json")) {
            response = mapper.readValue(json, WordCardResponse.class);
        }

        var card = response.toGeneratedWordCard();

        assertThat(card.lemma()).isEqualTo("borrow");
        assertThat(card.partOfSpeech()).isEqualTo("verb");
        assertThat(card.ipa()).isEqualTo("ˈbɒrəʊ");
        assertThat(card.meaning()).isEqualTo("брать взаймы");
        assertThat(card.examples()).hasSize(8);
        assertThat(card.examples().getFirst())
                .isEqualTo(new GeneratedExample("Can I borrow your pen?", "Можно взять твою ручку?", CefrLevel.A1));
        assertThat(card.examples()).extracting(GeneratedExample::level).isSorted();
    }

    @Test
    void missingExamplesBecomeEmptyList() throws IOException {
        var response = mapper.readValue("""
                {"lemma": "borrow", "partOfSpeech": "verb", "meaning": "брать взаймы"}
                """, WordCardResponse.class);

        assertThat(response.toGeneratedWordCard().examples()).isEmpty();
    }

    @Test
    void blankLemmaIsRejected() throws IOException {
        var response = mapper.readValue("""
                {"lemma": " ", "partOfSpeech": "verb", "meaning": "x", "examples": []}
                """, WordCardResponse.class);

        assertThatThrownBy(response::toGeneratedWordCard).isInstanceOf(IllegalArgumentException.class);
    }
}
