package dev.words.infra.llm;

import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Settings of one LLM provider, e.g. {@code words.llm.openai.*}.
 *
 * @param apiKey     API key; a provider without a key is skipped
 * @param model      model id
 * @param maxTokens  upper bound for one response
 * @param timeout    timeout of one HTTP request; retries add to the total wait
 * @param maxRetries retries on 408/409/429/5xx and connection errors before falling back to the next provider
 */
public record ProviderProperties(
        @DefaultValue("") String apiKey,
        String model,
        @DefaultValue("4000") long maxTokens,
        @DefaultValue("30s") Duration timeout,
        @DefaultValue("1") int maxRetries) {

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
