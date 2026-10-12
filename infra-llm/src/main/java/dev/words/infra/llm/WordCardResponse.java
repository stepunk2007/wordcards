package dev.words.infra.llm;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import dev.words.application.port.GeneratedExample;
import dev.words.application.port.GeneratedWordCard;
import dev.words.domain.CefrLevel;

import java.util.List;

/**
 * JSON contract of the LLM answer. The Claude SDK derives the structured output schema from this record,
 * so field names and descriptions here are part of the prompt.
 */
record WordCardResponse(
        @JsonPropertyDescription("true if the input is a real word or phrase of the learned language, "
                + "possibly with a typo or in an inflected form; false for other languages, gibberish or sentences. "
                + "When false, leave the other fields empty")
        boolean inLearnedLanguage,
        @JsonPropertyDescription("Dictionary form of the word or phrase, in the learned language")
        String lemma,
        @JsonPropertyDescription("Part of speech in English, e.g. noun, verb, adjective, phrasal verb, idiom")
        String partOfSpeech,
        @JsonPropertyDescription("Short meaning in the learner's native language, 1-5 words, the most common sense first")
        String meaning,
        @JsonPropertyDescription("Simple example sentences using the word, easiest first")
        List<ExampleResponse> examples) {

    record ExampleResponse(
            @JsonPropertyDescription("Sentence in the learned language")
            String text,
            @JsonPropertyDescription("Natural translation into the learner's native language")
            String translation,
            @JsonPropertyDescription("CEFR level of the sentence")
            CefrLevel level) {
    }

    /**
     * @throws IllegalArgumentException or NullPointerException if required fields are missing or blank
     */
    GeneratedWordCard toGeneratedWordCard() {
        var generatedExamples = examples == null ? List.<GeneratedExample>of() : examples.stream()
                .map(example -> new GeneratedExample(example.text(), example.translation(), example.level()))
                .toList();
        return new GeneratedWordCard(lemma, partOfSpeech, meaning, generatedExamples);
    }
}
