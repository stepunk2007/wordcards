package dev.words.infra.llm;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import dev.words.application.port.GeneratedExample;
import dev.words.application.port.LlmPort;
import dev.words.application.port.WordCardRequest;
import dev.words.domain.CefrLevel;
import dev.words.domain.Language;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Calls the real LLM APIs to eyeball the quality of generated examples. Costs money, so it is excluded
 * from {@code ./gradlew build}; run it with {@code ./gradlew :infra-llm:manualTest}.
 * API keys come from the environment or the repository's {@code .env}; a provider without a key is skipped.
 */
@Tag("manual")
class LlmManualTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    @Test
    void openAiGeneratesCardForBorrowed() {
        String apiKey = key("OPENAI_API_KEY").orElse("");
        assumeTrue(!apiKey.isBlank(), "OPENAI_API_KEY is not set");

        var client = OpenAIOkHttpClient.builder().apiKey(apiKey).timeout(TIMEOUT).maxRetries(1).build();
        try {
            check("openai", new OpenAiLlmAdapter(client,
                    new ProviderProperties(apiKey, "gpt-6-luna", 4000, TIMEOUT, 1)));
        } finally {
            client.close();
        }
    }

    @Test
    void anthropicGeneratesCardForBorrowed() {
        String apiKey = key("ANTHROPIC_API_KEY").orElse("");
        assumeTrue(!apiKey.isBlank(), "ANTHROPIC_API_KEY is not set");

        var client = AnthropicOkHttpClient.builder().apiKey(apiKey).timeout(TIMEOUT).maxRetries(1).build();
        try {
            check("anthropic", new ClaudeLlmAdapter(client,
                    new ProviderProperties(apiKey, "claude-haiku-5-5", 4000, TIMEOUT, 1)));
        } finally {
            client.close();
        }
    }

    private static void check(String provider, LlmPort adapter) {
        long started = System.nanoTime();
        var card = adapter.generateWordCard(
                new WordCardRequest("borrowed", Language.ENGLISH, Language.RUSSIAN, CefrLevel.A2));
        long millis = Duration.ofNanos(System.nanoTime() - started).toMillis();

        System.out.printf("%n[%s] %s /%s/ (%s): %s  [%d ms]%n",
                provider, card.lemma(), card.ipa(), card.partOfSpeech(), card.meaning(), millis);
        for (GeneratedExample example : card.examples()) {
            System.out.printf("  [%s] %s%n       %s%n", example.level(), example.text(), example.translation());
        }

        assertThat(card.lemma()).isEqualToIgnoringCase("borrow");
        assertThat(card.meaning()).isNotBlank();
        assertThat(card.examples()).hasSizeGreaterThanOrEqualTo(8);
        assertThat(card.examples()).allSatisfy(example -> {
            assertThat(example.text().toLowerCase()).contains("borrow");
            assertThat(example.translation()).isNotBlank();
            assertThat(example.level()).isLessThanOrEqualTo(CefrLevel.B1);
        });
    }

    private static Optional<String> key(String name) {
        String fromEnv = System.getenv(name);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return Optional.of(fromEnv);
        }
        Path dotEnv = Path.of("..", ".env");
        if (!Files.exists(dotEnv)) {
            return Optional.empty();
        }
        try (var lines = Files.lines(dotEnv)) {
            return lines.map(String::strip)
                    .filter(line -> line.startsWith(name + "="))
                    .map(line -> line.substring(name.length() + 1).strip())
                    .findFirst();
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
