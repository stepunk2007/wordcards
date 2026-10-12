package dev.words.application.word;

import dev.words.application.error.NoActiveProfileException;
import dev.words.application.error.NotFoundException;
import dev.words.application.port.ExampleRepository;
import dev.words.application.port.LearningProfileRepository;
import dev.words.application.port.LexemeRepository;
import dev.words.application.port.LlmPort;
import dev.words.application.port.MoreExamplesRequest;
import dev.words.application.port.UserRepository;
import dev.words.domain.Example;
import dev.words.domain.LearningProfile;
import dev.words.domain.Lexeme;
import dev.words.domain.WordNormalizer;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Returns the next portion of example sentences for a word. Examples are shared by all users;
 * when the cached ones run out, new ones are generated once and stored for everybody.
 */
public class GetMoreExamples {

    public static final int PAGE_SIZE = 3;
    /** Generating new examples costs money; past this many a word has enough. */
    static final int MAX_EXAMPLES = 30;
    static final int GENERATION_BATCH = 6;

    private final UserRepository users;
    private final LearningProfileRepository profiles;
    private final LexemeRepository lexemes;
    private final ExampleRepository examples;
    private final LlmPort llm;

    public GetMoreExamples(UserRepository users, LearningProfileRepository profiles, LexemeRepository lexemes,
                           ExampleRepository examples, LlmPort llm) {
        this.users = users;
        this.profiles = profiles;
        this.lexemes = lexemes;
        this.examples = examples;
        this.llm = llm;
    }

    /**
     * @param offset how many examples the user has already seen, e.g. 3 after the first card
     * @throws dev.words.application.port.LlmException if new examples were needed but could not be generated
     */
    public ExamplePage next(long userId, long lexemeId, int offset) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must not be negative: " + offset);
        }
        var profile = activeProfile(userId);
        var lexeme = lexemes.findById(lexemeId)
                .orElseThrow(() -> new NotFoundException("Word " + lexemeId + " not found"));

        var cached = examples.findByLexemeId(lexemeId);
        if (cached.size() < offset + PAGE_SIZE && cached.size() < MAX_EXAMPLES) {
            cached = generateMore(profile, lexeme, cached);
        }

        int from = Math.min(offset, cached.size());
        int to = Math.min(offset + PAGE_SIZE, cached.size());
        // An empty page means even generation found nothing new: stop offering the button.
        boolean hasMore = from < to && (to < cached.size() || cached.size() < MAX_EXAMPLES);
        return new ExamplePage(cached.subList(from, to), to, hasMore);
    }

    private List<Example> generateMore(LearningProfile profile, Lexeme lexeme, List<Example> cached) {
        int count = Math.min(GENERATION_BATCH, MAX_EXAMPLES - cached.size());
        var generated = llm.generateMoreExamples(new MoreExamplesRequest(
                lexeme.text(), lexeme.language(), profile.nativeLanguage(), profile.level(),
                cached.stream().map(Example::text).toList(), count));

        var seen = new HashSet<String>();
        cached.forEach(example -> seen.add(WordNormalizer.normalize(example.text())));
        var fresh = generated.stream()
                .filter(example -> seen.add(WordNormalizer.normalize(example.text())))
                .limit(count)
                .map(example -> new Example(null, lexeme.id(), example.text(), example.level(),
                        Map.of(profile.nativeLanguage(), example.translation())))
                .toList();
        if (fresh.isEmpty()) {
            return cached;
        }
        try {
            examples.saveAll(fresh);
        } catch (RuntimeException e) {
            // Another user's request saved the same sentences first; theirs are as good as ours.
        }
        return examples.findByLexemeId(lexeme.id());
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
}
