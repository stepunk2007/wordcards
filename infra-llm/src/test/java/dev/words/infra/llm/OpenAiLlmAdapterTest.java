package dev.words.infra.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.okhttp.OpenAIOkHttpClient;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs the OpenAI adapter against a local fake of the Chat Completions API.
 */
class OpenAiLlmAdapterTest {

    private static final WordCardRequest REQUEST =
            new WordCardRequest("borrow", Language.ENGLISH, Language.RUSSIAN, CefrLevel.A2);

    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer server;
    private volatile int status;
    private volatile String body;
    private volatile String lastRequest;
    private volatile Duration delay = Duration.ZERO;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            lastRequest = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
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
    void sendsPromptsAndSchemaAndParsesCard() throws IOException {
        respond(cardJson(true), "stop", null);

        var card = adapter(Duration.ofSeconds(5)).generateWordCard(REQUEST);

        assertThat(card.lemma()).isEqualTo("borrow");
        assertThat(card.ipa()).isEqualTo("ˈbɒrəʊ");
        assertThat(card.examples()).hasSize(1);
        var request = mapper.readTree(lastRequest);
        assertThat(request.get("model").asText()).isEqualTo("gpt-6-luna");
        assertThat(request.get("messages").get(0).get("content").asText()).isEqualTo(WordCardPrompt.SYSTEM);
        assertThat(request.get("messages").get(1).get("content").asText()).startsWith("Word: borrow");
        assertThat(request.get("response_format").toString()).contains("json_schema", "inLearnedLanguage", "examples");
    }

    @Test
    void returnsMoreExamples() throws IOException {
        respond(mapper.writeValueAsString(Map.of("examples", List.of(Map.of(
                "text", "I borrowed his bike.", "translation", "Я взял его велосипед.", "level", "A2")))),
                "stop", null);

        var examples = adapter(Duration.ofSeconds(5)).generateMoreExamples(new MoreExamplesRequest(
                "borrow", Language.ENGLISH, Language.RUSSIAN, CefrLevel.A2, List.of(), 3));

        assertThat(examples).extracting(GeneratedExample::text).containsExactly("I borrowed his bike.");
    }

    @Test
    void wordInAnotherLanguageIsReported() throws IOException {
        respond(cardJson(false), "stop", null);

        assertThatThrownBy(() -> adapter(Duration.ofSeconds(5)).generateWordCard(REQUEST))
                .isInstanceOf(WordNotInLanguageException.class);
    }

    @Test
    void slowAnswerIsTimeout() throws IOException {
        respond(cardJson(true), "stop", null);
        delay = Duration.ofSeconds(2);

        assertReason(Duration.ofMillis(300), Reason.TIMEOUT);
    }

    @Test
    void truncatedAnswerIsInvalidResponse() throws IOException {
        respond("{\"inLearnedLanguage\": true, \"lemma\": \"bor", "length", null);

        assertReason(Duration.ofSeconds(5), Reason.INVALID_RESPONSE);
    }

    @Test
    void refusalIsReported() throws IOException {
        respond(null, "stop", "I can't help with that.");

        assertReason(Duration.ofSeconds(5), Reason.REFUSED);
    }

    @Test
    void outageIsUnavailable() throws IOException {
        respondWithError(503, "server_error");

        assertReason(Duration.ofSeconds(5), Reason.UNAVAILABLE);
    }

    @Test
    void rateLimitIsUnavailable() throws IOException {
        respondWithError(429, "rate_limit_exceeded");

        assertReason(Duration.ofSeconds(5), Reason.UNAVAILABLE);
    }

    @Test
    void emptyBalanceIsMisconfiguration() throws IOException {
        respondWithError(429, "insufficient_quota");

        assertReason(Duration.ofSeconds(5), Reason.MISCONFIGURED);
    }

    @Test
    void invalidKeyIsMisconfiguration() throws IOException {
        respondWithError(401, "invalid_api_key");

        assertReason(Duration.ofSeconds(5), Reason.MISCONFIGURED);
    }

    private void assertReason(Duration timeout, Reason reason) {
        assertThatThrownBy(() -> adapter(timeout).generateWordCard(REQUEST))
                .isInstanceOfSatisfying(LlmException.class, e -> assertThat(e.reason()).isEqualTo(reason));
    }

    private OpenAiLlmAdapter adapter(Duration timeout) {
        var properties = new ProviderProperties("sk-test", "gpt-6-luna", 4000, timeout, 0);
        var client = OpenAIOkHttpClient.builder()
                .apiKey("sk-test")
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .timeout(timeout)
                .maxRetries(0)
                .build();
        return new OpenAiLlmAdapter(client, properties);
    }

    private String cardJson(boolean inLearnedLanguage) throws IOException {
        return mapper.writeValueAsString(Map.of(
                "inLearnedLanguage", inLearnedLanguage,
                "lemma", "borrow",
                "partOfSpeech", "verb",
                "ipa", "ˈbɒrəʊ",
                "meaning", "брать взаймы",
                "examples", List.of(Map.of(
                        "text", "Can I borrow your pen?",
                        "translation", "Можно взять твою ручку?",
                        "level", "A1"))));
    }

    private void respond(String content, String finishReason, String refusal) throws IOException {
        status = 200;
        var message = new HashMap<String, Object>();
        message.put("role", "assistant");
        message.put("content", content);
        message.put("refusal", refusal);
        var choice = new HashMap<String, Object>();
        choice.put("index", 0);
        choice.put("message", message);
        choice.put("finish_reason", finishReason);
        choice.put("logprobs", null);
        body = mapper.writeValueAsString(Map.of(
                "id", "chatcmpl-test",
                "object", "chat.completion",
                "created", 1760000000,
                "model", "gpt-6-luna",
                "choices", List.of(choice),
                "usage", Map.of("prompt_tokens", 10, "completion_tokens", 10, "total_tokens", 20)));
    }

    private void respondWithError(int httpStatus, String code) throws IOException {
        status = httpStatus;
        body = mapper.writeValueAsString(Map.of(
                "error", Map.of("message", "test error", "type", code, "code", code)));
    }
}
