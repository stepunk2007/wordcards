package dev.words.infra.llm;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import dev.words.application.port.GeneratedExample;

import java.util.List;

/**
 * JSON contract of the answer with extra example sentences.
 */
record ExamplesResponse(
        @JsonPropertyDescription("New example sentences, easiest first")
        List<WordCardResponse.ExampleResponse> examples) {

    List<GeneratedExample> toGeneratedExamples() {
        return examples == null ? List.of() : examples.stream()
                .map(example -> new GeneratedExample(example.text(), example.translation(), example.level()))
                .toList();
    }
}
