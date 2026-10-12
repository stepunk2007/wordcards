package dev.words.infra.llm;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import dev.words.application.port.GeneratedExample;
import dev.words.application.port.LlmException;
import dev.words.application.port.LlmException.Reason;
import dev.words.application.port.MoreExamplesRequest;
import dev.words.application.port.WordCardRequest;
import dev.words.application.port.WordNotInLanguageException;
import dev.words.domain.CefrLevel;
import dev.words.domain.Language;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs the adapter against a local fake of the Messages API to check how failures are reported.
 */
class ClaudeLlmAdapterErrorsTest {

    private static final WordCardRequest REQUEST =
            new WordCardRequest("borrow", Language.ENGLISH, Language.RUSSIAN, CefrLevel.A2);

    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer server;
    private volatile int status;
    private volatile String body;
    private volatile Duration delay = Duration.ZERO;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            try {
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
            } catch (IOException ignored) {
                // the client gave up waiting
            }
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void returnsCardForValidAnswer() throws IOException {
        respondWithText(cardJson(true), "end_turn");

        var card = adapter(Duration.ofSeconds(5)).generateWordCard(REQUEST);

        assertThat(card.lemma()).isEqualTo("borrow");
        assertThat(card.examples()).hasSize(1);
    }

    @Test
    void wordInAnotherLanguageIsReported() throws IOException {
        respondWithText(cardJson(false), "end_turn");

        assertThatThrownBy(() -> adapter(Duration.ofSeconds(5)).generateWordCard(REQUEST))
                .isInstanceOfSatisfying(WordNotInLanguageException.class, e -> {
                    assertThat(e.text()).isEqualTo("borrow");
                    assertThat(e.expectedLanguage()).isEqualTo(Language.ENGLISH);
                });
    }

    @Test
    void slowAnswerIsTimeout() throws IOException {
        respondWithText(cardJson(true), "end_turn");
        delay = Duration.ofSeconds(2);

        assertReason(Duration.ofMillis(300), Reason.TIMEOUT);
    }

    @Test
    void textThatIsNotJsonIsInvalidResponse() throws IOException {
        respondWithText("Sure! Here is your card: borrow", "end_turn");

        assertReason(Duration.ofSeconds(5), Reason.INVALID_RESPONSE);
    }

    @Test
    void jsonWithoutRequiredFieldsIsInvalidResponse() throws IOException {
        respondWithText("{\"inLearnedLanguage\": true, \"lemma\": \"\", \"meaning\": null, \"examples\": []}", "end_turn");

        assertReason(Duration.ofSeconds(5), Reason.INVALID_RESPONSE);
    }

    @Test
    void truncatedAnswerIsInvalidResponse() throws IOException {
        respondWithText("{\"inLearnedLanguage\": true, \"lemma\": \"bor", "max_tokens");

        assertReason(Duration.ofSeconds(5), Reason.INVALID_RESPONSE);
    }

    @Test
    void refusalIsReported() throws IOException {
        respondWithText("", "refusal");

        assertReason(Duration.ofSeconds(5), Reason.REFUSED);
    }

    @Test
    void overloadedApiIsUnavailable() throws IOException {
        respondWithError(529, "overloaded_error");

        assertReason(Duration.ofSeconds(5), Reason.UNAVAILABLE);
    }

    @Test
    void rateLimitIsUnavailable() throws IOException {
        respondWithError(429, "rate_limit_error");

        assertReason(Duration.ofSeconds(5), Reason.UNAVAILABLE);
    }

    @Test
    void invalidApiKeyIsMisconfiguration() throws IOException {
        respondWithError(401, "authentication_error");

        assertReason(Duration.ofSeconds(5), Reason.MISCONFIGURED);
    }

    @Test
    void returnsMoreExamples() throws IOException {
        respondWithText(mapper.writeValueAsString(Map.of("examples", java.util.List.of(Map.of(
                "text", "I borrowed his bike.", "translation", "Я взял его велосипед.", "level", "A2")))), "end_turn");

        var examples = adapter(Duration.ofSeconds(5)).generateMoreExamples(new MoreExamplesRequest(
                "borrow", Language.ENGLISH, Language.RUSSIAN, CefrLevel.A2, java.util.List.of(), 3));

        assertThat(examples).extracting(GeneratedExample::text).containsExactly("I borrowed his bike.");
    }

    @Test
    void moreExamplesFailuresUseSameReasons() throws IOException {
        respondWithError(529, "overloaded_error");

        assertThatThrownBy(() -> adapter(Duration.ofSeconds(5)).generateMoreExamples(new MoreExamplesRequest(
                "borrow", Language.ENGLISH, Language.RUSSIAN, CefrLevel.A2, java.util.List.of(), 3)))
                .isInstanceOfSatisfying(LlmException.class, e -> assertThat(e.reason()).isEqualTo(Reason.UNAVAILABLE));
    }

    @Test
    void reasonsTellWhetherRetryHelps() {
        assertThat(Reason.TIMEOUT.retryable()).isTrue();
        assertThat(Reason.UNAVAILABLE.retryable()).isTrue();
        assertThat(Reason.MISCONFIGURED.retryable()).isFalse();
        assertThat(Reason.REFUSED.retryable()).isFalse();
    }

    private void assertReason(Duration timeout, Reason reason) {
        assertThatThrownBy(() -> adapter(timeout).generateWordCard(REQUEST))
                .isInstanceOfSatisfying(LlmException.class, e -> assertThat(e.reason()).isEqualTo(reason));
    }

    private ClaudeLlmAdapter adapter(Duration timeout) {
        var properties = new LlmProperties("sk-test", "claude-haiku-5-5", 4000, timeout, 0);
        var client = AnthropicOkHttpClient.builder()
                .apiKey("sk-test")
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .timeout(timeout)
                .maxRetries(0)
                .build();
        return new ClaudeLlmAdapter(client, properties);
    }

    private String cardJson(boolean inLearnedLanguage) throws IOException {
        return mapper.writeValueAsString(Map.of(
                "inLearnedLanguage", inLearnedLanguage,
                "lemma", "borrow",
                "partOfSpeech", "verb",
                "meaning", "брать взаймы",
                "examples", java.util.List.of(Map.of(
                        "text", "Can I borrow your pen?",
                        "translation", "Можно взять твою ручку?",
                        "level", "A1"))));
    }

    private void respondWithText(String text, String stopReason) throws IOException {
        status = 200;
        body = mapper.writeValueAsString(Map.of(
                "id", "msg_test",
                "type", "message",
                "role", "assistant",
                "model", "claude-haiku-5-5",
                "content", text.isEmpty()
                        ? java.util.List.of()
                        : java.util.List.of(Map.of("type", "text", "text", text)),
                "stop_reason", stopReason,
                "usage", Map.of("input_tokens", 10, "output_tokens", 10)));
    }

    private void respondWithError(int httpStatus, String type) throws IOException {
        status = httpStatus;
        body = mapper.writeValueAsString(Map.of(
                "type", "error",
                "error", Map.of("type", type, "message", "test error")));
    }
}
