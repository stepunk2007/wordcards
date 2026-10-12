package dev.words.application.word;

import dev.words.application.error.NoActiveProfileException;
import dev.words.application.error.NotFoundException;
import dev.words.application.port.ExampleRepository;
import dev.words.application.port.GeneratedWordCard;
import dev.words.application.port.LearningProfileRepository;
import dev.words.application.port.LexemeRepository;
import dev.words.application.port.LlmPort;
import dev.words.application.port.UserCardRepository;
import dev.words.application.port.UserRepository;
import dev.words.application.port.WordCardRequest;
import dev.words.domain.Example;
import dev.words.domain.LearningProfile;
import dev.words.domain.Lexeme;
import dev.words.domain.UserCard;
import dev.words.domain.WordNormalizer;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Adds a word to the deck of the user's active profile.
 * <p>
 * Lexemes are shared by all users: the LLM is called only for a word nobody has added before.
 * Adding a word that is already in the deck changes nothing and returns the existing card.
 */
public class AddWord {

    static final int MAX_LENGTH = 100;

    private final UserRepository users;
    private final LearningProfileRepository profiles;
    private final LexemeRepository lexemes;
    private final ExampleRepository examples;
    private final UserCardRepository cards;
    private final LlmPort llm;
    private final PronunciationResolver pronunciations;
    private final Clock clock;
    private final int dailyLimit;

    /**
     * @param dailyLimit new words a user may add per day across all languages; 0 or less means no limit
     */
    public AddWord(UserRepository users, LearningProfileRepository profiles, LexemeRepository lexemes,
                   ExampleRepository examples, UserCardRepository cards, LlmPort llm,
                   PronunciationResolver pronunciations, Clock clock, int dailyLimit) {
        this.dailyLimit = dailyLimit;
        this.users = users;
        this.profiles = profiles;
        this.lexemes = lexemes;
        this.examples = examples;
        this.cards = cards;
        this.llm = llm;
        this.pronunciations = pronunciations;
        this.clock = clock;
    }

    /**
     * @throws InvalidWordException                                  if the text is blank or too long
     * @throws DailyLimitExceededException                           if the user added too many new words today
     * @throws NoActiveProfileException                              if the user has not chosen a language yet
     * @throws dev.words.application.port.WordNotInLanguageException if the text is not a word of that language
     * @throws dev.words.application.port.LlmException               if a new card could not be generated
     */
    public AddedWord add(long userId, String text) {
        String typed = validate(text);
        var profile = activeProfile(userId);

        var cached = lexemes.findByLanguageAndNormalizedText(profile.targetLanguage(), WordNormalizer.normalize(typed));
        if (cached.isPresent()) {
            var inDeck = cards.findByProfileIdAndLexemeId(profile.id(), cached.get().id());
            if (inDeck.isPresent()) {
                return result(profile, cached.get(), inDeck.get(), true);
            }
        }

        // Checked before the LLM call: a word over the limit must not cost money.
        checkDailyLimit(userId);
        var lexeme = cached.orElseGet(() -> generate(profile, typed));

        var existing = cards.findByProfileIdAndLexemeId(profile.id(), lexeme.id());
        if (existing.isPresent()) {
            return result(profile, lexeme, existing.get(), true);
        }
        var card = saveOrFind(
                () -> cards.save(UserCard.newCard(profile.id(), lexeme.id(), clock.instant())),
                () -> cards.findByProfileIdAndLexemeId(profile.id(), lexeme.id()));
        return result(profile, lexeme, card, false);
    }

    private AddedWord result(LearningProfile profile, Lexeme lexeme, UserCard card, boolean alreadyInDeck) {
        return new AddedWord(profile, lexeme, examples.findByLexemeId(lexeme.id()), card, alreadyInDeck);
    }

    /**
     * Counts new words in all of the user's languages since midnight UTC.
     */
    private void checkDailyLimit(long userId) {
        if (dailyLimit <= 0) {
            return;
        }
        var startOfDay = clock.instant().truncatedTo(ChronoUnit.DAYS);
        var profileIds = profiles.findByUserId(userId).stream().map(LearningProfile::id).toList();
        if (cards.countCreatedSince(profileIds, startOfDay) >= dailyLimit) {
            throw new DailyLimitExceededException(dailyLimit);
        }
    }

    private static String validate(String text) {
        if (text == null || text.isBlank()) {
            throw new InvalidWordException("Word is empty");
        }
        String typed = text.strip();
        if (typed.length() > MAX_LENGTH) {
            throw new InvalidWordException("Word is longer than " + MAX_LENGTH + " characters");
        }
        return typed;
    }

    private LearningProfile activeProfile(long userId) {
        var user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " not found"));
        if (!user.hasActiveProfile()) {
            throw new NoActiveProfileException(userId);
        }
        return profiles.findById(user.activeProfileId())
                .orElseThrow(() -> new NoActiveProfileException(userId));
    }

    private Lexeme generate(LearningProfile profile, String typed) {
        var generated = llm.generateWordCard(new WordCardRequest(
                typed, profile.targetLanguage(), profile.nativeLanguage(), profile.level()));

        // "borrowed" is generated as "borrow": another user may have added the lemma already.
        String lemmaKey = WordNormalizer.normalize(generated.lemma());
        var existingLemma = lexemes.findByLanguageAndNormalizedText(profile.targetLanguage(), lemmaKey);
        if (existingLemma.isPresent()) {
            return existingLemma.get();
        }

        return saveOrFind(
                () -> saveNewLexeme(profile, typed, generated),
                () -> lexemes.findByLanguageAndNormalizedText(profile.targetLanguage(), lemmaKey));
    }

    private Lexeme saveNewLexeme(LearningProfile profile, String typed, GeneratedWordCard generated) {
        var pronunciation = pronunciations.resolve(profile.targetLanguage(), generated.lemma(), typed, generated.ipa());
        var lexeme = lexemes.save(Lexeme.create(
                profile.targetLanguage(),
                generated.lemma(),
                generated.partOfSpeech(),
                pronunciation.map(p -> p.ipa()).orElse(null),
                pronunciation.map(p -> p.source()).orElse(null),
                Map.of(profile.nativeLanguage(), generated.meaning())));

        var seenTexts = new HashSet<String>();
        examples.saveAll(generated.examples().stream()
                .filter(example -> seenTexts.add(example.text().strip()))
                .map(example -> new Example(null, lexeme.id(), example.text(), example.level(),
                        Map.of(profile.nativeLanguage(), example.translation())))
                .toList());
        return lexeme;
    }

    /**
     * Saves, and if a concurrent request saved the same row first (unique key violation), returns that row.
     */
    private static <T> T saveOrFind(Supplier<T> save, Supplier<Optional<T>> find) {
        try {
            return save.get();
        } catch (RuntimeException e) {
            return find.get().orElseThrow(() -> e);
        }
    }
}
