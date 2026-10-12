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

    public AddWord(UserRepository users, LearningProfileRepository profiles, LexemeRepository lexemes,
                   ExampleRepository examples, UserCardRepository cards, LlmPort llm,
                   PronunciationResolver pronunciations, Clock clock) {
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
     * @throws NoActiveProfileException                              if the user has not chosen a language yet
     * @throws dev.words.application.port.WordNotInLanguageException if the text is not a word of that language
     * @throws dev.words.application.port.LlmException               if a new card could not be generated
     */
    public AddedWord add(long userId, String text) {
        String typed = validate(text);
        var profile = activeProfile(userId);

        var lexeme = lexemes.findByLanguageAndNormalizedText(profile.targetLanguage(), WordNormalizer.normalize(typed))
                .orElseGet(() -> generate(profile, typed));

        var existing = cards.findByProfileIdAndLexemeId(profile.id(), lexeme.id());
        var card = existing.orElseGet(() -> saveOrFind(
                () -> cards.save(UserCard.newCard(profile.id(), lexeme.id(), clock.instant())),
                () -> cards.findByProfileIdAndLexemeId(profile.id(), lexeme.id())));

        return new AddedWord(profile, lexeme, examples.findByLexemeId(lexeme.id()), card, existing.isPresent());
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
