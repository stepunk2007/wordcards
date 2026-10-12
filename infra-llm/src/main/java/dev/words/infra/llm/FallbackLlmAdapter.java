package dev.words.infra.llm;

import dev.words.application.port.GeneratedExample;
import dev.words.application.port.GeneratedWordCard;
import dev.words.application.port.LlmException;
import dev.words.application.port.LlmException.Reason;
import dev.words.application.port.LlmPort;
import dev.words.application.port.MoreExamplesRequest;
import dev.words.application.port.WordCardRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Function;

/**
 * Tries LLM providers in order and returns the first answer. Any {@link LlmException} of one provider
 * (timeout, outage, bad key, broken answer, refusal) moves on to the next one. A verdict about the input itself,
 * such as {@link dev.words.application.port.WordNotInLanguageException}, is final and is not retried elsewhere.
 */
class FallbackLlmAdapter implements LlmPort {

    private static final Logger log = LoggerFactory.getLogger(FallbackLlmAdapter.class);

    /**
     * @param name     provider name for logs, e.g. "openai"
     * @param provider the provider's adapter
     */
    record Provider(String name, LlmPort provider) {
    }

    private final List<Provider> providers;

    FallbackLlmAdapter(List<Provider> providers) {
        this.providers = List.copyOf(providers);
    }

    @Override
    public GeneratedWordCard generateWordCard(WordCardRequest request) {
        return firstAnswer(provider -> provider.generateWordCard(request));
    }

    @Override
    public List<GeneratedExample> generateMoreExamples(MoreExamplesRequest request) {
        return firstAnswer(provider -> provider.generateMoreExamples(request));
    }

    private <T> T firstAnswer(Function<LlmPort, T> call) {
        if (providers.isEmpty()) {
            throw new LlmException(Reason.MISCONFIGURED, "No LLM provider has an API key");
        }
        LlmException last = null;
        for (int i = 0; i < providers.size(); i++) {
            var provider = providers.get(i);
            try {
                return call.apply(provider.provider());
            } catch (LlmException e) {
                last = e;
                if (i + 1 < providers.size()) {
                    log.warn("LLM provider {} failed ({}: {}), trying {}", provider.name(), e.reason(),
                            e.getMessage(), providers.get(i + 1).name());
                }
            }
        }
        throw last;
    }
}
