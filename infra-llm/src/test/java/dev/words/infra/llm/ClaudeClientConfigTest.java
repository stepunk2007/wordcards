package dev.words.infra.llm;

import com.anthropic.client.AnthropicClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ClaudeClientConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ClaudeClientConfig.class);

    @Test
    void usesHaikuAndSafeDefaults() {
        contextRunner.run(context -> {
            var properties = context.getBean(LlmProperties.class);

            assertThat(properties.model()).isEqualTo("claude-haiku-5-5");
            assertThat(properties.timeout()).isEqualTo(Duration.ofSeconds(30));
            assertThat(properties.maxRetries()).isEqualTo(2);
            assertThat(properties.hasApiKey()).isFalse();
        });
    }

    @Test
    void startsWithoutApiKey() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(AnthropicClient.class));
    }

    @Test
    void bindsSettingsFromProperties() {
        contextRunner
                .withPropertyValues(
                        "words.llm.api-key=sk-test",
                        "words.llm.model=claude-sonnet-5-5",
                        "words.llm.timeout=10s",
                        "words.llm.max-retries=0",
                        "words.llm.max-tokens=2000")
                .run(context -> {
                    var properties = context.getBean(LlmProperties.class);

                    assertThat(properties.hasApiKey()).isTrue();
                    assertThat(properties.model()).isEqualTo("claude-sonnet-5-5");
                    assertThat(properties.timeout()).isEqualTo(Duration.ofSeconds(10));
                    assertThat(properties.maxRetries()).isZero();
                    assertThat(properties.maxTokens()).isEqualTo(2000);
                    assertThat(context).hasSingleBean(AnthropicClient.class);
                });
    }
}
