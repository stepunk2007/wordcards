package dev.words.infra.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.errors.AnthropicException;
import com.anthropic.errors.AnthropicInvalidDataException;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.BadRequestException;
import com.anthropic.errors.CredentialResolutionException;
import com.anthropic.errors.NoCredentialsException;
import com.anthropic.errors.NotFoundException;
import com.anthropic.errors.PermissionDeniedException;
import com.anthropic.errors.UnauthorizedException;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.StructuredOutputConfig;
import dev.words.application.port.GeneratedExample;
import dev.words.application.port.GeneratedWordCard;
import dev.words.application.port.LlmException;
import dev.words.application.port.LlmException.Reason;
import dev.words.application.port.LlmPort;
import dev.words.application.port.MoreExamplesRequest;
import dev.words.application.port.WordCardRequest;
import dev.words.application.port.WordNotInLanguageException;

import java.io.InterruptedIOException;
import java.util.List;

/**
 * Generates word cards with Claude using structured output, so the answer always matches the response records.
 * SDK errors are translated into {@link LlmException} so callers never see provider-specific types.
 */
class ClaudeLlmAdapter implements LlmPort {

    private final AnthropicClient client;
    private final ProviderProperties properties;

    ClaudeLlmAdapter(AnthropicClient client, ProviderProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public GeneratedWordCard generateWordCard(WordCardRequest request) {
        var response = call(buildParams(request), "a card for '" + request.text() + "'");
        if (!response.inLearnedLanguage()) {
            throw new WordNotInLanguageException(request.text(), request.language());
        }
        try {
            return response.toGeneratedWordCard();
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new LlmException(Reason.INVALID_RESPONSE, "Claude answer misses required fields", e);
        }
    }

    @Override
    public List<GeneratedExample> generateMoreExamples(MoreExamplesRequest request) {
        var response = call(buildParams(request), "examples for '" + request.lemma() + "'");
        try {
            return response.toGeneratedExamples();
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new LlmException(Reason.INVALID_RESPONSE, "Claude answer misses required fields", e);
        }
    }

    StructuredMessageCreateParams<WordCardResponse> buildParams(WordCardRequest request) {
        return params(WordCardResponse.class, WordCardPrompt.SYSTEM, WordCardPrompt.userMessage(request));
    }

    StructuredMessageCreateParams<ExamplesResponse> buildParams(MoreExamplesRequest request) {
        return params(ExamplesResponse.class, WordCardPrompt.MORE_EXAMPLES_SYSTEM,
                WordCardPrompt.moreExamplesMessage(request));
    }

    private <T> StructuredMessageCreateParams<T> params(Class<T> responseType, String system, String userMessage) {
        return MessageCreateParams.builder()
                .model(properties.model())
                .maxTokens(properties.maxTokens())
                .system(system)
                // Simple generation task: low effort keeps latency and cost down.
                .outputConfig(new StructuredOutputConfig.Builder<T>()
                        .format(responseType)
                        .effort(OutputConfig.Effort.LOW)
                        .build())
                .addUserMessage(userMessage)
                .build();
    }

    private <T> T call(StructuredMessageCreateParams<T> params, String what) {
        StructuredMessage<T> message;
        try {
            message = client.messages().create(params);
        } catch (AnthropicException e) {
            throw translate(e);
        }

        var stopReason = message.stopReason().orElse(null);
        if (StopReason.REFUSAL.equals(stopReason)) {
            throw new LlmException(Reason.REFUSED, "Claude refused to generate " + what);
        }
        if (!StopReason.END_TURN.equals(stopReason)) {
            throw new LlmException(Reason.INVALID_RESPONSE, "Claude answer is incomplete, stop reason: " + stopReason);
        }

        try {
            return message.content().stream()
                    .flatMap(block -> block.text().stream())
                    .findFirst()
                    .orElseThrow(() -> new LlmException(Reason.INVALID_RESPONSE, "Claude answer has no text"))
                    .text();
        } catch (AnthropicInvalidDataException e) {
            throw new LlmException(Reason.INVALID_RESPONSE, "Claude answer is not valid JSON", e);
        }
    }

    private static LlmException translate(AnthropicException e) {
        return switch (e) {
            case AnthropicIoException io when hasCause(io, InterruptedIOException.class) ->
                    new LlmException(Reason.TIMEOUT, "Claude did not answer in time", e);
            case UnauthorizedException _, PermissionDeniedException _, NotFoundException _, BadRequestException _,
                 NoCredentialsException _, CredentialResolutionException _ ->
                    new LlmException(Reason.MISCONFIGURED, "Claude API rejected the request: " + e.getMessage(), e);
            case AnthropicInvalidDataException _ ->
                    new LlmException(Reason.INVALID_RESPONSE, "Claude answer is not valid JSON", e);
            default -> new LlmException(Reason.UNAVAILABLE, "Claude API is unavailable: " + e.getMessage(), e);
        };
    }

    private static boolean hasCause(Throwable error, Class<? extends Throwable> type) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (type.isInstance(cause)) {
                return true;
            }
        }
        return false;
    }
}
