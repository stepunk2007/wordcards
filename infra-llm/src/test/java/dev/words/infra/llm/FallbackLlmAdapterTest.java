package dev.words.infra.llm;

import dev.words.application.port.GeneratedExample;
import dev.words.application.port.GeneratedWordCard;
import dev.words.application.port.LlmException;
import dev.words.application.port.LlmException.Reason;
import dev.words.application.port.LlmPort;
import dev.words.application.port.MoreExamplesRequest;
import dev.words.application.port.WordCardRequest;
import dev.words.application.port.WordNotInLanguageException;
import dev.words.domain.CefrLevel;
import dev.words.domain.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FallbackLlmAdapterTest {

    private static final WordCardRequest REQUEST =
            new WordCardRequest("borrow", Language.ENGLISH, Language.RUSSIAN, CefrLevel.A2);
    private static final MoreExamplesRequest MORE =
            new MoreExamplesRequest("borrow", Language.ENGLISH, Language.RUSSIAN, CefrLevel.A2, List.of(), 3);

    @Test
    void mainProviderAnswersAndFallbackIsNotCalled() {
        var openai = new StubProvider("openai");
        var anthropic = new StubProvider("anthropic");

        var card = adapter(openai, anthropic).generateWordCard(REQUEST);

        assertThat(card.meaning()).isEqualTo("openai");
        assertThat(anthropic.calls).isZero();
    }

    @ParameterizedTest
    @EnumSource(Reason.class)
    void anyLlmFailureOfMainProviderFallsBackToNext(Reason reason) {
        var openai = new StubProvider("openai");
        openai.failure = new LlmException(reason, "openai failed");

        var card = adapter(openai, new StubProvider("anthropic")).generateWordCard(REQUEST);

        assertThat(card.meaning()).isEqualTo("anthropic");
    }

    @Test
    void moreExamplesFallBackToo() {
        var openai = new StubProvider("openai");
        openai.failure = new LlmException(Reason.UNAVAILABLE, "down");

        var examples = adapter(openai, new StubProvider("anthropic")).generateMoreExamples(MORE);

        assertThat(examples).extracting(GeneratedExample::text).containsExactly("anthropic example");
    }

    @Test
    void wordInAnotherLanguageIsFinalAndNotAskedAgain() {
        var openai = new StubProvider("openai");
        openai.failure = new WordNotInLanguageException("книга", Language.ENGLISH);
        var anthropic = new StubProvider("anthropic");

        assertThatThrownBy(() -> adapter(openai, anthropic).generateWordCard(REQUEST))
                .isInstanceOf(WordNotInLanguageException.class);
        assertThat(anthropic.calls).isZero();
    }

    @Test
    void whenAllFailTheLastErrorIsReported() {
        var openai = new StubProvider("openai");
        openai.failure = new LlmException(Reason.TIMEOUT, "slow");
        var anthropic = new StubProvider("anthropic");
        anthropic.failure = new LlmException(Reason.MISCONFIGURED, "bad key");

        assertThatThrownBy(() -> adapter(openai, anthropic).generateWordCard(REQUEST))
                .isInstanceOfSatisfying(LlmException.class, e -> assertThat(e.reason()).isEqualTo(Reason.MISCONFIGURED));
    }

    @Test
    void withoutProvidersGenerationIsMisconfigured() {
        assertThatThrownBy(() -> new FallbackLlmAdapter(List.of()).generateWordCard(REQUEST))
                .isInstanceOfSatisfying(LlmException.class, e -> assertThat(e.reason()).isEqualTo(Reason.MISCONFIGURED));
    }

    private static FallbackLlmAdapter adapter(StubProvider... providers) {
        return new FallbackLlmAdapter(List.of(providers).stream()
                .map(stub -> new FallbackLlmAdapter.Provider(stub.name, stub))
                .toList());
    }

    private static class StubProvider implements LlmPort {

        final String name;
        RuntimeException failure;
        int calls;

        StubProvider(String name) {
            this.name = name;
        }

        @Override
        public GeneratedWordCard generateWordCard(WordCardRequest request) {
            calls++;
            if (failure != null) {
                throw failure;
            }
            return new GeneratedWordCard("borrow", "verb", null, name, List.of());
        }

        @Override
        public List<GeneratedExample> generateMoreExamples(MoreExamplesRequest request) {
            calls++;
            if (failure != null) {
                throw failure;
            }
            return List.of(new GeneratedExample(name + " example", "пример", CefrLevel.A1));
        }
    }
}
