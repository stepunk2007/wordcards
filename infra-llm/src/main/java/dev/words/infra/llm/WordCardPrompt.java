package dev.words.infra.llm;

import dev.words.application.port.WordCardRequest;
import dev.words.domain.Language;

import java.util.Locale;

/**
 * Prompt for generating a word card. The system part is constant so it can be cached;
 * everything request-specific goes into the user message.
 */
final class WordCardPrompt {

    static final int EXAMPLE_COUNT = 8;

    static final String SYSTEM = """
            You write vocabulary cards for a language learning app.

            First check that the input is a word or phrase of the learned language. Accept typos and inflected \
            forms. If it is in another language, is gibberish or is a whole sentence, set inLearnedLanguage to \
            false, leave the other fields empty and stop.

            For the word or phrase the learner sends:
            - Give its dictionary form (lemma): "borrowed" becomes "borrow", "went" becomes "go". \
            Keep phrases and phrasal verbs whole.
            - Give the IPA transcription of the lemma, without slashes; British pronunciation for English.
            - Give a short meaning in the learner's native language: 1-5 words, the most common sense first.
            - Write %d example sentences that use the word.

            Example sentences:
            - Short and simple: everyday situations, common vocabulary, at or below the learner's level.
            - Show the word in different forms and contexts (tenses, plural, questions, negatives).
            - Never use rarer words than the word itself.
            - Each sentence has a natural, not word-for-word, translation into the native language.
            - Order them from the easiest to the hardest.
            """.formatted(EXAMPLE_COUNT);

    private WordCardPrompt() {
    }

    static String userMessage(WordCardRequest request) {
        return """
                Word: %s
                Learned language: %s
                Native language: %s
                Learner's level: %s""".formatted(
                request.text().strip(),
                languageName(request.language()),
                languageName(request.nativeLanguage()),
                request.level());
    }

    private static String languageName(Language language) {
        return Locale.forLanguageTag(language.code()).getDisplayLanguage(Locale.ENGLISH);
    }
}
