package dev.words.application.word;

import dev.words.application.fake.InMemoryRepositories;
import dev.words.application.port.GeneratedWordCard;
import dev.words.application.port.LlmPort;
import dev.words.application.port.MoreExamplesRequest;
import dev.words.application.port.WordCardRequest;
import dev.words.application.port.GeneratedExample;
import dev.words.application.profile.CreateProfile;
import dev.words.domain.CefrLevel;
import dev.words.domain.Language;
import dev.words.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AddWordDailyLimitTest {

    private static final Instant MORNING = Instant.parse("2026-10-12T08:00:00Z");
    private static final int LIMIT = 2;

    private final InMemoryRepositories.Users users = new InMemoryRepositories.Users();
    private final InMemoryRepositories.Profiles profiles = new InMemoryRepositories.Profiles();
    private final InMemoryRepositories.Lexemes lexemes = new InMemoryRepositories.Lexemes();
    private final InMemoryRepositories.Examples examples = new InMemoryRepositories.Examples();
    private final InMemoryRepositories.Cards cards = new InMemoryRepositories.Cards();
    private final EchoLlm llm = new EchoLlm();
    private final MutableClock clock = new MutableClock(MORNING);

    private long userId;

    @BeforeEach
    void setUp() {
        userId = users.save(User.register(42, MORNING)).id();
        new CreateProfile(users, profiles, clock).create(userId, Language.ENGLISH, Language.RUSSIAN, CefrLevel.A2);
    }

    @Test
    void refusesNewWordOverTheLimitWithoutCallingLlm() {
        var addWord = addWord(LIMIT);
        addWord.add(userId, "one");
        addWord.add(userId, "two");

        assertThatThrownBy(() -> addWord.add(userId, "three"))
                .isInstanceOfSatisfying(DailyLimitExceededException.class, e -> assertThat(e.limit()).isEqualTo(2));
        assertThat(llm.calls).isEqualTo(2);
        assertThat(cards.all).hasSize(2);
    }

    @Test
    void wordAlreadyInDeckCanBeOpenedOverTheLimit() {
        var addWord = addWord(LIMIT);
        addWord.add(userId, "one");
        addWord.add(userId, "two");

        assertThat(addWord.add(userId, "one").alreadyInDeck()).isTrue();
    }

    @Test
    void limitCountsAllLanguagesOfTheUser() {
        var addWord = addWord(LIMIT);
        addWord.add(userId, "one");
        var russian = new CreateProfile(users, profiles, clock)
                .create(userId, Language.RUSSIAN, Language.ENGLISH, CefrLevel.A1);
        new dev.words.application.profile.SwitchProfile(users, profiles).switchTo(userId, russian.id());
        addWord.add(userId, "два");

        assertThatThrownBy(() -> addWord.add(userId, "три")).isInstanceOf(DailyLimitExceededException.class);
    }

    @Test
    void limitResetsAtMidnightUtc() {
        var addWord = addWord(LIMIT);
        addWord.add(userId, "one");
        addWord.add(userId, "two");

        clock.now = Instant.parse("2026-10-13T00:00:00Z");

        assertThat(addWord.add(userId, "three").alreadyInDeck()).isFalse();
    }

    @Test
    void otherUsersDoNotCount() {
        var addWord = addWord(LIMIT);
        long other = users.save(User.register(43, MORNING)).id();
        new CreateProfile(users, profiles, clock).create(other, Language.ENGLISH, Language.RUSSIAN, CefrLevel.A2);
        addWord.add(other, "one");
        addWord.add(other, "two");

        assertThat(addWord.add(userId, "three").alreadyInDeck()).isFalse();
    }

    @Test
    void zeroMeansNoLimit() {
        var addWord = addWord(0);

        for (String word : List.of("one", "two", "three", "four")) {
            addWord.add(userId, word);
        }

        assertThat(cards.all).hasSize(4);
    }

    private AddWord addWord(int limit) {
        var pronunciations = new PronunciationResolver((language, text) -> Optional.empty());
        return new AddWord(users, profiles, lexemes, examples, cards, llm, pronunciations, clock, limit);
    }

    /** Returns the typed word as its own lemma. */
    private static class EchoLlm implements LlmPort {

        int calls;

        @Override
        public GeneratedWordCard generateWordCard(WordCardRequest request) {
            calls++;
            return new GeneratedWordCard(request.text(), "noun", null, "перевод", List.of());
        }

        @Override
        public List<GeneratedExample> generateMoreExamples(MoreExamplesRequest request) {
            throw new UnsupportedOperationException();
        }
    }

    private static class MutableClock extends Clock {

        Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
