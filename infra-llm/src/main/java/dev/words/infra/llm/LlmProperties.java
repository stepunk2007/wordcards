package dev.words.infra.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Claude API settings, bound from {@code words.llm.*}.
 *
 * @param apiKey     Claude API key; empty until configured, then LLM calls fail but the app still starts
 * @param model      model id used for word cards
 * @param maxTokens  upper bound for one response
 * @param timeout    timeout of one HTTP request; retries add to the total wait
 * @param maxRetries retries on 408/409/429/5xx and connection errors
 */
@ConfigurationProperties("words.llm")
public record LlmProperties(
        @DefaultValue("") String apiKey,
        @DefaultValue("claude-haiku-5-5") String model,
        @DefaultValue("4000") long maxTokens,
        @DefaultValue("30s") Duration timeout,
        @DefaultValue("2") int maxRetries) {

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
