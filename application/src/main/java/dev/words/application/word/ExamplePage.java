package dev.words.application.word;

import dev.words.domain.Example;
import dev.words.domain.Language;

import java.util.List;

/**
 * A portion of example sentences for the "More examples" button.
 *
 * @param nativeLanguage language of the translations to show
 * @param nextOffset offset to request the following page with
 * @param hasMore    false when the word has no more examples to show and none will be generated
 */
public record ExamplePage(List<Example> examples, Language nativeLanguage, int nextOffset, boolean hasMore) {

    public ExamplePage {
        examples = List.copyOf(examples);
    }
}
