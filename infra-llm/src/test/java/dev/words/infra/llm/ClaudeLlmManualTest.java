package dev.words.infra.llm;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import dev.words.application.port.GeneratedExample;
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
 * Calls the real Claude API to eyeball the quality of generated examples. Costs money, so it is excluded
 * from {@code ./gradlew build}; run it with {@code ./gradlew :infra-llm:manualTest}.
 * The API key comes from {@code ANTHROPIC_API_KEY} or the repository's {@code .env}.
 */
@Tag("manual")
class ClaudeLlmManualTest {

    @Test
    void generatesCardForBorrow() {
        String apiKey = apiKey().orElse("");
        assumeTrue(!apiKey.isBlank(), "ANTHROPIC_API_KEY is not set");

        var properties = new LlmProperties(apiKey, "claude-haiku-5-5", 4000, Duration.ofSeconds(60), 1);
        var client = AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .timeout(properties.timeout())
                .maxRetries(properties.maxRetries())
                .build();
        try {
            var adapter = new ClaudeLlmAdapter(client, properties);

            long started = System.nanoTime();
            var card = adapter.generateWordCard(
                    new WordCardRequest("borrowed", Language.ENGLISH, Language.RUSSIAN, CefrLevel.A2));
            long millis = Duration.ofNanos(System.nanoTime() - started).toMillis();

            System.out.printf("%n%s (%s): %s  [%d ms]%n", card.lemma(), card.partOfSpeech(), card.meaning(), millis);
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
        } finally {
            client.close();
        }
    }

    private static Optional<String> apiKey() {
        String fromEnv = System.getenv("ANTHROPIC_API_KEY");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return Optional.of(fromEnv);
        }
        Path dotEnv = Path.of("..", ".env");
        if (!Files.exists(dotEnv)) {
            return Optional.empty();
        }
        try (var lines = Files.lines(dotEnv)) {
            return lines.map(String::strip)
                    .filter(line -> line.startsWith("ANTHROPIC_API_KEY="))
                    .map(line -> line.substring("ANTHROPIC_API_KEY=".length()).strip())
                    .findFirst();
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
