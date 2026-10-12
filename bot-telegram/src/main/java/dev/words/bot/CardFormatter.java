package dev.words.bot;

import dev.words.application.word.AddedWord;
import dev.words.domain.Example;
import dev.words.domain.Language;

import java.util.List;

/**
 * Renders a word card as Telegram HTML.
 */
final class CardFormatter {

    static final int EXAMPLES_ON_CARD = 3;

    private CardFormatter() {
    }

    static String card(AddedWord word) {
        var lexeme = word.lexeme();
        Language nativeLanguage = word.profile().nativeLanguage();
        var text = new StringBuilder();

        text.append("<b>").append(escape(lexeme.text())).append("</b>");
        if (lexeme.ipa() != null) {
            text.append("  /").append(escape(lexeme.ipa())).append("/");
        }
        if (lexeme.partOfSpeech() != null && !lexeme.partOfSpeech().isBlank()) {
            text.append("  · <i>").append(escape(lexeme.partOfSpeech())).append("</i>");
        }
        lexeme.meaningIn(nativeLanguage).ifPresent(meaning -> text.append('\n').append(escape(meaning)));

        List<Example> examples = word.examples().stream().limit(EXAMPLES_ON_CARD).toList();
        if (!examples.isEmpty()) {
            text.append('\n');
            for (int i = 0; i < examples.size(); i++) {
                text.append('\n').append(example(i + 1, examples.get(i), nativeLanguage));
            }
        }

        if (word.alreadyInDeck()) {
            text.append("\n\n<i>Это слово уже в твоей колоде.</i>");
        }
        return text.toString();
    }

    static String example(int number, Example example, Language nativeLanguage) {
        var text = new StringBuilder()
                .append(number).append(". ").append(escape(example.text()));
        example.translationIn(nativeLanguage)
                .ifPresent(translation -> text.append("\n    <i>").append(escape(translation)).append("</i>"));
        return text.toString();
    }

    static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
