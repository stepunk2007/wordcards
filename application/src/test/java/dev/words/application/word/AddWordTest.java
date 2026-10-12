package dev.words.application.word;

import dev.words.application.error.NoActiveProfileException;
import dev.words.application.fake.InMemoryRepositories;
import dev.words.application.port.GeneratedExample;
import dev.words.application.port.GeneratedWordCard;
import dev.words.application.port.LlmException;
import dev.words.application.port.LlmPort;
import dev.words.application.port.Pronunciation;
import dev.words.application.port.WordCardRequest;
import dev.words.application.port.WordNotInLanguageException;
import dev.words.application.profile.CreateProfile;
import dev.words.domain.CardState;
import dev.words.domain.CefrLevel;
import dev.words.domain.IpaSource;
import dev.words.domain.Language;
import dev.words.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AddWordTest {

    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final InMemoryRepositories.Users users = new InMemoryRepositories.Users();
    private final InMemoryRepositories.Profiles profiles = new InMemoryRepositories.Profiles();
    private final InMemoryRepositories.Lexemes lexemes = new InMemoryRepositories.Lexemes();
    private final InMemoryRepositories.Examples examples = new InMemoryRepositories.Examples();
    private final InMemoryRepositories.Cards cards = new InMemoryRepositories.Cards();
    private final FakeLlm llm = new FakeLlm();
    private final Map<String, String> dictionary = Map.of("borrow", "ˈbɒɹəʊ");
    private final PronunciationResolver pronunciations = new PronunciationResolver((language, text) ->
            Optional.ofNullable(dictionary.get(text)).map(ipa -> new Pronunciation(ipa, IpaSource.WIKTIONARY)));
    private final AddWord addWord =
            new AddWord(users, profiles, lexemes, examples, cards, llm, pronunciations, CLOCK);
    private final CreateProfile createProfile = new CreateProfile(users, profiles, CLOCK);

    private long userId;

    @BeforeEach
    void setUp() {
        userId = users.save(User.register(42, NOW)).id();
        createProfile.create(userId, Language.ENGLISH, Language.RUSSIAN, CefrLevel.A2);
    }

    @Test
    void newWordIsGeneratedSavedAndAddedToDeck() {
        var added = addWord.add(userId, "  Borrow ");

        assertThat(llm.requests).containsExactly(
                new WordCardRequest("Borrow", Language.ENGLISH, Language.RUSSIAN, CefrLevel.A2));
        assertThat(added.alreadyInDeck()).isFalse();
        assertThat(added.lexeme().text()).isEqualTo("borrow");
        assertThat(added.lexeme().ipa()).isEqualTo("ˈbɒɹəʊ");
        assertThat(added.lexeme().ipaSource()).isEqualTo(IpaSource.WIKTIONARY);
        assertThat(added.lexeme().meaningIn(Language.RUSSIAN)).contains("брать взаймы");
        assertThat(added.examples()).hasSize(2);
        assertThat(added.examples().getFirst().translationIn(Language.RUSSIAN)).contains("Можно взять твою ручку?");
        assertThat(added.card().state()).isEqualTo(CardState.NEW);
        assertThat(added.card().dueAt()).isEqualTo(NOW);
        assertThat(added.card().profileId()).isEqualTo(added.profile().id());
    }

    @Test
    void addingSameWordAgainCreatesNoDuplicateAndNoLlmCall() {
        var first = addWord.add(userId, "borrow");
        var second = addWord.add(userId, "BORROW");

        assertThat(second.alreadyInDeck()).isTrue();
        assertThat(second.card()).isEqualTo(first.card());
        assertThat(llm.requests).hasSize(1);
        assertThat(cards.all).hasSize(1);
        assertThat(lexemes.byId).hasSize(1);
    }

    @Test
    void wordAddedByAnotherUserIsReusedWithoutLlm() {
        addWord.add(userId, "borrow");
        long otherUserId = users.save(User.register(43, NOW)).id();
        createProfile.create(otherUserId, Language.ENGLISH, Language.RUSSIAN, CefrLevel.B1);

        var added = addWord.add(otherUserId, "borrow");

        assertThat(added.alreadyInDeck()).isFalse();
        assertThat(llm.requests).hasSize(1);
        assertThat(cards.all).hasSize(2);
        assertThat(examples.all).hasSize(2);
    }

    @Test
    void inflectedFormIsStoredAsLemmaAndReusesExistingLexeme() {
        var borrowed = addWord.add(userId, "borrowed");
        var borrow = addWord.add(userId, "borrow");

        assertThat(borrowed.lexeme().normalizedText()).isEqualTo("borrow");
        assertThat(borrow.lexeme()).isEqualTo(borrowed.lexeme());
        assertThat(borrow.alreadyInDeck()).isTrue();
        assertThat(lexemes.byId).hasSize(1);
    }

    @Test
    void usesLlmIpaWhenDictionaryLacksTheWord() {
        llm.lemma = "serendipity";
        llm.ipa = "ˌsɛrənˈdɪpɪti";

        var added = addWord.add(userId, "serendipity");

        assertThat(added.lexeme().ipa()).isEqualTo("ˌsɛrənˈdɪpɪti");
        assertThat(added.lexeme().ipaSource()).isEqualTo(IpaSource.LLM);
    }

    @Test
    void duplicateExampleTextsAreSavedOnce() {
        llm.examples = List.of(
                new GeneratedExample("Can I borrow your pen?", "Можно взять твою ручку?", CefrLevel.A1),
                new GeneratedExample("Can I borrow your pen?", "Можно одолжить твою ручку?", CefrLevel.A1));

        assertThat(addWord.add(userId, "borrow").examples()).hasSize(1);
    }

    @Test
    void wordInAnotherLanguageIsNotSaved() {
        llm.failure = new WordNotInLanguageException("книга", Language.ENGLISH);

        assertThatThrownBy(() -> addWord.add(userId, "книга")).isInstanceOf(WordNotInLanguageException.class);
        assertThat(lexemes.byId).isEmpty();
        assertThat(cards.all).isEmpty();
    }

    @Test
    void llmFailureIsPassedThroughAndNothingIsSaved() {
        llm.failure = new LlmException(LlmException.Reason.TIMEOUT, "slow");

        assertThatThrownBy(() -> addWord.add(userId, "borrow")).isInstanceOf(LlmException.class);
        assertThat(lexemes.byId).isEmpty();
    }

    @Test
    void userWithoutProfileCannotAddWords() {
        long newcomer = users.save(User.register(44, NOW)).id();

        assertThatThrownBy(() -> addWord.add(newcomer, "borrow")).isInstanceOf(NoActiveProfileException.class);
        assertThat(llm.requests).isEmpty();
    }

    @Test
    void rejectsBlankAndTooLongText() {
        assertThatThrownBy(() -> addWord.add(userId, "   ")).isInstanceOf(InvalidWordException.class);
        assertThatThrownBy(() -> addWord.add(userId, "a".repeat(AddWord.MAX_LENGTH + 1)))
                .isInstanceOf(InvalidWordException.class);
        assertThat(llm.requests).isEmpty();
    }

    private static class FakeLlm implements LlmPort {

        final List<WordCardRequest> requests = new ArrayList<>();
        String lemma = "borrow";
        String ipa = "ˈbɒrəʊ";
        List<GeneratedExample> examples = List.of(
                new GeneratedExample("Can I borrow your pen?", "Можно взять твою ручку?", CefrLevel.A1),
                new GeneratedExample("She borrowed a book.", "Она взяла книгу.", CefrLevel.A2));
        RuntimeException failure;

        @Override
        public GeneratedWordCard generateWordCard(WordCardRequest request) {
            requests.add(request);
            if (failure != null) {
                throw failure;
            }
            return new GeneratedWordCard(lemma, "verb", ipa, "брать взаймы", examples);
        }
    }
}
