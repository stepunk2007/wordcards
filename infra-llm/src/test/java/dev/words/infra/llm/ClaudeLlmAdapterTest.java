package dev.words.infra.llm;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import dev.words.application.port.MoreExamplesRequest;
import dev.words.application.port.WordCardRequest;
import dev.words.domain.CefrLevel;
import dev.words.domain.Language;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClaudeLlmAdapterTest {

    private final ProviderProperties properties =
            new ProviderProperties("sk-test", "claude-haiku-5-5", 4000, Duration.ofSeconds(30), 2);
    private final ClaudeLlmAdapter adapter =
            new ClaudeLlmAdapter(AnthropicOkHttpClient.builder().apiKey("sk-test").build(), properties);

    @Test
    void buildsRequestWithConfiguredModelPromptAndSchema() {
        var params = adapter.buildParams(
                new WordCardRequest(" borrowed ", Language.ENGLISH, Language.RUSSIAN, CefrLevel.A2)).rawParams();

        assertThat(params.model().asString()).isEqualTo("claude-haiku-5-5");
        assertThat(params.maxTokens()).isEqualTo(4000);
        assertThat(params.system().orElseThrow().asString()).isEqualTo(WordCardPrompt.SYSTEM);

        String schema = params.outputConfig().orElseThrow().toString();
        assertThat(schema).contains("lemma", "partOfSpeech", "ipa", "meaning", "examples", "translation", "A1", "C2");

        String userMessage = params.messages().getFirst().content().asString();
        assertThat(userMessage).isEqualTo("""
                Word: borrowed
                Learned language: English
                Native language: Russian
                Learner's level: A2""");
    }

    @Test
    void buildsMoreExamplesRequestListingExistingSentences() {
        var params = adapter.buildParams(new MoreExamplesRequest("borrow", Language.ENGLISH, Language.RUSSIAN,
                CefrLevel.A2, List.of("Can I borrow your pen?", "She borrowed a book."), 6)).rawParams();

        assertThat(params.system().orElseThrow().asString()).isEqualTo(WordCardPrompt.MORE_EXAMPLES_SYSTEM);
        assertThat(params.outputConfig().orElseThrow().toString()).contains("examples", "translation");
        assertThat(params.messages().getFirst().content().asString()).isEqualTo("""
                Word: borrow
                Learned language: English
                Native language: Russian
                Learner's level: A2
                Number of new sentences: 6
                Existing sentences:
                - Can I borrow your pen?
                - She borrowed a book.""");
    }

    @Test
    void systemPromptAsksForEightSimpleExamples() {
        assertThat(WordCardPrompt.SYSTEM).contains("Write 8 example sentences", "lemma");
    }
}
