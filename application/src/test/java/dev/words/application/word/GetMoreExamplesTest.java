package dev.words.application.word;

import dev.words.application.error.NotFoundException;
import dev.words.application.fake.InMemoryRepositories;
import dev.words.application.port.GeneratedExample;
import dev.words.application.port.GeneratedWordCard;
import dev.words.application.port.LlmException;
import dev.words.application.port.LlmPort;
import dev.words.application.port.MoreExamplesRequest;
import dev.words.application.port.WordCardRequest;
import dev.words.application.profile.CreateProfile;
import dev.words.domain.CefrLevel;
import dev.words.domain.Example;
import dev.words.domain.Language;
import dev.words.domain.Lexeme;
import dev.words.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetMoreExamplesTest {

    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");

    private final InMemoryRepositories.Users users = new InMemoryRepositories.Users();
    private final InMemoryRepositories.Profiles profiles = new InMemoryRepositories.Profiles();
    private final InMemoryRepositories.Lexemes lexemes = new InMemoryRepositories.Lexemes();
    private final InMemoryRepositories.Examples examples = new InMemoryRepositories.Examples();
    private final FakeLlm llm = new FakeLlm();
    private final GetMoreExamples getMoreExamples = new GetMoreExamples(users, profiles, lexemes, examples, llm);

    private long userId;
    private long lexemeId;

    @BeforeEach
    void setUp() {
        userId = users.save(User.register(42, NOW)).id();
        new CreateProfile(users, profiles, Clock.fixed(NOW, ZoneOffset.UTC))
                .create(userId, Language.ENGLISH, Language.RUSSIAN, CefrLevel.A2);
        lexemeId = lexemes.save(Lexeme.create(Language.ENGLISH, "borrow", "verb", null, null, Map.of())).id();
    }

    @Test
    void servesCachedExamplesPageByPageWithoutLlm() {
        cache(9);

        var second = getMoreExamples.next(userId, lexemeId, 3);
        var third = getMoreExamples.next(userId, lexemeId, second.nextOffset());

        assertThat(second.examples()).extracting(Example::text).containsExactly("cached 3", "cached 4", "cached 5");
        assertThat(second.nextOffset()).isEqualTo(6);
        assertThat(third.examples()).extracting(Example::text).containsExactly("cached 6", "cached 7", "cached 8");
        assertThat(llm.requests).isEmpty();
    }

    @Test
    void generatesMoreWhenCacheRunsOutAndStoresThemForEverybody() {
        cache(8);

        var page = getMoreExamples.next(userId, lexemeId, 6);

        var request = llm.requests.getFirst();
        assertThat(request.lemma()).isEqualTo("borrow");
        assertThat(request.nativeLanguage()).isEqualTo(Language.RUSSIAN);
        assertThat(request.level()).isEqualTo(CefrLevel.A2);
        assertThat(request.existing()).hasSize(8);
        assertThat(request.count()).isEqualTo(GetMoreExamples.GENERATION_BATCH);
        assertThat(page.examples()).extracting(Example::text).containsExactly("cached 6", "cached 7", "new 0");
        assertThat(page.examples().getLast().translationIn(Language.RUSSIAN)).contains("новый 0");
        assertThat(page.nativeLanguage()).isEqualTo(Language.RUSSIAN);
        assertThat(page.hasMore()).isTrue();
        assertThat(examples.findByLexemeId(lexemeId)).hasSize(14);
    }

    @Test
    void lexemeWithoutExamplesGetsThemGenerated() {
        var page = getMoreExamples.next(userId, lexemeId, 0);

        assertThat(page.examples()).extracting(Example::text).containsExactly("new 0", "new 1", "new 2");
    }

    @Test
    void duplicatesOfExistingSentencesAreDropped() {
        cache(3);
        llm.texts = List.of("Cached 0", "new 0", "new 0");

        var page = getMoreExamples.next(userId, lexemeId, 3);

        assertThat(page.examples()).extracting(Example::text).containsExactly("new 0");
    }

    @Test
    void stopsGeneratingAtTheLimit() {
        cache(GetMoreExamples.MAX_EXAMPLES);

        var last = getMoreExamples.next(userId, lexemeId, GetMoreExamples.MAX_EXAMPLES - 3);
        var beyond = getMoreExamples.next(userId, lexemeId, GetMoreExamples.MAX_EXAMPLES);

        assertThat(last.examples()).hasSize(3);
        assertThat(last.hasMore()).isFalse();
        assertThat(beyond.examples()).isEmpty();
        assertThat(beyond.hasMore()).isFalse();
        assertThat(llm.requests).isEmpty();
    }

    @Test
    void emptyGenerationEndsTheList() {
        cache(3);
        llm.texts = List.of();

        var page = getMoreExamples.next(userId, lexemeId, 3);

        assertThat(page.examples()).isEmpty();
        assertThat(page.hasMore()).isFalse();
    }

    @Test
    void llmFailureIsPassedThrough() {
        llm.failure = new LlmException(LlmException.Reason.UNAVAILABLE, "down");

        assertThatThrownBy(() -> getMoreExamples.next(userId, lexemeId, 0)).isInstanceOf(LlmException.class);
    }

    @Test
    void unknownWordIsNotFound() {
        assertThatThrownBy(() -> getMoreExamples.next(userId, 999, 0)).isInstanceOf(NotFoundException.class);
    }

    private void cache(int count) {
        examples.saveAll(IntStream.range(0, count)
                .mapToObj(i -> new Example(null, lexemeId, "cached " + i, CefrLevel.A1, Map.of(Language.RUSSIAN, "x")))
                .toList());
    }

    private static class FakeLlm implements LlmPort {

        final List<MoreExamplesRequest> requests = new ArrayList<>();
        List<String> texts;
        RuntimeException failure;

        @Override
        public GeneratedWordCard generateWordCard(WordCardRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<GeneratedExample> generateMoreExamples(MoreExamplesRequest request) {
            requests.add(request);
            if (failure != null) {
                throw failure;
            }
            var result = texts != null ? texts : IntStream.range(0, request.count()).mapToObj(i -> "new " + i).toList();
            return IntStream.range(0, result.size())
                    .mapToObj(i -> new GeneratedExample(result.get(i), "новый " + i, CefrLevel.A2))
                    .toList();
        }
    }
}
