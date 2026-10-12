package dev.words.infra.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import dev.words.application.port.LlmPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds the {@link LlmPort}: the providers listed in {@code words.llm.providers} that have an API key,
 * tried in that order.
 */
@Configuration
@EnableConfigurationProperties(LlmProperties.class)
class LlmConfig {

    private static final Logger log = LoggerFactory.getLogger(LlmConfig.class);

    @Bean(destroyMethod = "close")
    OpenAIClient openAiClient(LlmProperties properties) {
        var settings = properties.openai();
        return OpenAIOkHttpClient.builder()
                .apiKey(settings.hasApiKey() ? settings.apiKey() : "not-configured")
                .timeout(settings.timeout())
                .maxRetries(settings.maxRetries())
                .build();
    }

    @Bean(destroyMethod = "close")
    AnthropicClient anthropicClient(LlmProperties properties) {
        var settings = properties.anthropic();
        return AnthropicOkHttpClient.builder()
                .apiKey(settings.hasApiKey() ? settings.apiKey() : "not-configured")
                .timeout(settings.timeout())
                .maxRetries(settings.maxRetries())
                .build();
    }

    @Bean
    LlmPort llmPort(LlmProperties properties, OpenAIClient openAi, AnthropicClient anthropic) {
        List<FallbackLlmAdapter.Provider> providers = new ArrayList<>();
        for (String name : properties.providers()) {
            String key = name.strip().toLowerCase(Locale.ROOT);
            switch (key) {
                case "openai" -> {
                    if (properties.openai().hasApiKey()) {
                        providers.add(new FallbackLlmAdapter.Provider(key,
                                new OpenAiLlmAdapter(openAi, properties.openai())));
                    }
                }
                case "anthropic" -> {
                    if (properties.anthropic().hasApiKey()) {
                        providers.add(new FallbackLlmAdapter.Provider(key,
                                new ClaudeLlmAdapter(anthropic, properties.anthropic())));
                    }
                }
                default -> throw new IllegalStateException("Unknown LLM provider in words.llm.providers: " + name);
            }
        }
        var names = providers.stream().map(FallbackLlmAdapter.Provider::name).toList();
        if (names.isEmpty()) {
            log.warn("No LLM provider has an API key (OPENAI_API_KEY, ANTHROPIC_API_KEY): "
                    + "word card generation will fail");
        } else {
            log.info("LLM providers in order: {}", names);
        }
        return new FallbackLlmAdapter(providers);
    }
}
