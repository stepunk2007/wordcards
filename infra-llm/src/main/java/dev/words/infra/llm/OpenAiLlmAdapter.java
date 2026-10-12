package dev.words.infra.llm;

import com.openai.client.OpenAIClient;
import com.openai.errors.BadRequestException;
import com.openai.errors.NotFoundException;
import com.openai.errors.OpenAIException;
import com.openai.errors.OpenAIInvalidDataException;
import com.openai.errors.OpenAIIoException;
import com.openai.errors.PermissionDeniedException;
import com.openai.errors.RateLimitException;
import com.openai.errors.UnauthorizedException;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.StructuredChatCompletion;
import com.openai.models.chat.completions.StructuredChatCompletionCreateParams;
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
 * Generates word cards with OpenAI chat completions and structured output: the same prompts and JSON contract
 * as {@link ClaudeLlmAdapter}, so both providers give interchangeable answers.
 */
class OpenAiLlmAdapter implements LlmPort {

    private final OpenAIClient client;
    private final ProviderProperties properties;

    OpenAiLlmAdapter(OpenAIClient client, ProviderProperties properties) {
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
            throw new LlmException(Reason.INVALID_RESPONSE, "OpenAI answer misses required fields", e);
        }
    }

    @Override
    public List<GeneratedExample> generateMoreExamples(MoreExamplesRequest request) {
        var response = call(buildParams(request), "examples for '" + request.lemma() + "'");
        try {
            return response.toGeneratedExamples();
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new LlmException(Reason.INVALID_RESPONSE, "OpenAI answer misses required fields", e);
        }
    }

    StructuredChatCompletionCreateParams<WordCardResponse> buildParams(WordCardRequest request) {
        return params(WordCardResponse.class, WordCardPrompt.SYSTEM, WordCardPrompt.userMessage(request));
    }

    StructuredChatCompletionCreateParams<ExamplesResponse> buildParams(MoreExamplesRequest request) {
        return params(ExamplesResponse.class, WordCardPrompt.MORE_EXAMPLES_SYSTEM,
                WordCardPrompt.moreExamplesMessage(request));
    }

    private <T> StructuredChatCompletionCreateParams<T> params(Class<T> responseType, String system,
                                                                String userMessage) {
        return ChatCompletionCreateParams.builder()
                .model(properties.model())
                .maxCompletionTokens(properties.maxTokens())
                .addSystemMessage(system)
                .addUserMessage(userMessage)
                .responseFormat(responseType)
                .build();
    }

    private <T> T call(StructuredChatCompletionCreateParams<T> params, String what) {
        StructuredChatCompletion<T> completion;
        try {
            completion = client.chat().completions().create(params);
        } catch (OpenAIException e) {
            throw translate(e);
        }

        var choice = completion.choices().stream().findFirst()
                .orElseThrow(() -> new LlmException(Reason.INVALID_RESPONSE, "OpenAI answer has no choices"));
        var finishReason = choice.finishReason();
        if (ChatCompletion.Choice.FinishReason.CONTENT_FILTER.equals(finishReason)
                || choice.message().refusal().isPresent()) {
            throw new LlmException(Reason.REFUSED, "OpenAI refused to generate " + what);
        }
        if (!ChatCompletion.Choice.FinishReason.STOP.equals(finishReason)) {
            throw new LlmException(Reason.INVALID_RESPONSE, "OpenAI answer is incomplete, finish reason: "
                    + finishReason);
        }
        try {
            return choice.message().content()
                    .orElseThrow(() -> new LlmException(Reason.INVALID_RESPONSE, "OpenAI answer has no text"));
        } catch (OpenAIInvalidDataException e) {
            throw new LlmException(Reason.INVALID_RESPONSE, "OpenAI answer is not valid JSON", e);
        }
    }

    private static LlmException translate(OpenAIException e) {
        return switch (e) {
            case OpenAIIoException io when hasCause(io, InterruptedIOException.class) ->
                    new LlmException(Reason.TIMEOUT, "OpenAI did not answer in time", e);
            // 429 is also how OpenAI reports an empty balance; waiting will not help, the operator must pay.
            case RateLimitException rate when rate.code().filter("insufficient_quota"::equals).isPresent() ->
                    new LlmException(Reason.MISCONFIGURED, "OpenAI account has no credits: " + e.getMessage(), e);
            case UnauthorizedException _, PermissionDeniedException _, NotFoundException _, BadRequestException _ ->
                    new LlmException(Reason.MISCONFIGURED, "OpenAI API rejected the request: " + e.getMessage(), e);
            case OpenAIInvalidDataException _ ->
                    new LlmException(Reason.INVALID_RESPONSE, "OpenAI answer is not valid JSON", e);
            default -> new LlmException(Reason.UNAVAILABLE, "OpenAI API is unavailable: " + e.getMessage(), e);
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
