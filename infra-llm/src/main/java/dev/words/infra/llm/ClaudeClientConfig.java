package dev.words.infra.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LlmProperties.class)
class ClaudeClientConfig {

    private static final Logger log = LoggerFactory.getLogger(ClaudeClientConfig.class);

    @Bean(destroyMethod = "close")
    AnthropicClient anthropicClient(LlmProperties properties) {
        if (!properties.hasApiKey()) {
            log.warn("words.llm.api-key (ANTHROPIC_API_KEY) is not set: word card generation will fail");
        }
        return AnthropicOkHttpClient.builder()
                .apiKey(properties.apiKey())
                .timeout(properties.timeout())
                .maxRetries(properties.maxRetries())
                .build();
    }
}
