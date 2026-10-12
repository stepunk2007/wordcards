package dev.words.infra.llm;

import dev.words.application.port.LlmPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LlmConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(LlmConfig.class);

    @Test
    void openAiFirstThenAnthropicByDefault() {
        contextRunner
                .withPropertyValues("words.llm.openai.api-key=sk-openai", "words.llm.anthropic.api-key=sk-ant")
                .run(context -> {
                    assertThat(providerNames(context.getBean(LlmPort.class))).containsExactly("openai", "anthropic");
                    var properties = context.getBean(LlmProperties.class);
                    assertThat(properties.openai().timeout()).isEqualTo(Duration.ofSeconds(30));
                    assertThat(properties.openai().maxRetries()).isEqualTo(1);
                });
    }

    @Test
    void providerWithoutKeyIsSkipped() {
        contextRunner
                .withPropertyValues("words.llm.anthropic.api-key=sk-ant")
                .run(context -> assertThat(providerNames(context.getBean(LlmPort.class)))
                        .containsExactly("anthropic"));
    }

    @Test
    void orderComesFromConfiguration() {
        contextRunner
                .withPropertyValues(
                        "words.llm.providers=anthropic,openai",
                        "words.llm.openai.api-key=sk-openai",
                        "words.llm.anthropic.api-key=sk-ant")
                .run(context -> assertThat(providerNames(context.getBean(LlmPort.class)))
                        .containsExactly("anthropic", "openai"));
    }

    @Test
    void startsWithoutAnyKey() {
        contextRunner.run(context -> assertThat(providerNames(context.getBean(LlmPort.class))).isEmpty());
    }

    @Test
    void unknownProviderFailsStartup() {
        contextRunner
                .withPropertyValues("words.llm.providers=openai,gemini", "words.llm.openai.api-key=sk-openai")
                .run(context -> assertThat(context).hasFailed());
    }

    @SuppressWarnings("unchecked")
    private static List<String> providerNames(LlmPort port) {
        var providers = (List<FallbackLlmAdapter.Provider>) ReflectionTestUtils.getField(port, "providers");
        return providers.stream().map(FallbackLlmAdapter.Provider::name).toList();
    }
}
