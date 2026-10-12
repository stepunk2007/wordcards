package dev.words.infra.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.StructuredOutputConfig;
import dev.words.application.port.GeneratedWordCard;
import dev.words.application.port.LlmPort;
import dev.words.application.port.WordCardRequest;
import org.springframework.stereotype.Component;

/**
 * Generates word cards with Claude using structured output, so the answer always matches {@link WordCardResponse}.
 */
@Component
class ClaudeLlmAdapter implements LlmPort {

    private final AnthropicClient client;
    private final LlmProperties properties;

    ClaudeLlmAdapter(AnthropicClient client, LlmProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public GeneratedWordCard generateWordCard(WordCardRequest request) {
        var response = client.messages().create(buildParams(request));
        return response.content().stream()
                .flatMap(block -> block.text().stream())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Claude returned no text, stop reason: " + response.stopReason().orElse(null)))
                .text()
                .toGeneratedWordCard();
    }

    StructuredMessageCreateParams<WordCardResponse> buildParams(WordCardRequest request) {
        return MessageCreateParams.builder()
                .model(properties.model())
                .maxTokens(properties.maxTokens())
                .system(WordCardPrompt.SYSTEM)
                // Simple generation task: low effort keeps latency and cost down.
                .outputConfig(new StructuredOutputConfig.Builder<WordCardResponse>()
                        .format(WordCardResponse.class)
                        .effort(OutputConfig.Effort.LOW)
                        .build())
                .addUserMessage(WordCardPrompt.userMessage(request))
                .build();
    }
}
