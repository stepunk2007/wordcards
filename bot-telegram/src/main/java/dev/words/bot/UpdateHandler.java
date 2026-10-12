package dev.words.bot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Locale;

/**
 * Routes an update to the flow that answers it. Answers go through {@link Replies}, so it is tested without
 * a network.
 */
@Component
class UpdateHandler {

    private final TelegramProperties properties;
    private final Onboarding onboarding;
    private final WordFlow wordFlow;
    private final MoreExamplesFlow moreExamples;
    private final WordsListFlow wordsList;

    UpdateHandler(TelegramProperties properties, Onboarding onboarding, WordFlow wordFlow,
                  MoreExamplesFlow moreExamples, WordsListFlow wordsList) {
        this.properties = properties;
        this.onboarding = onboarding;
        this.wordFlow = wordFlow;
        this.moreExamples = moreExamples;
        this.wordsList = wordsList;
    }

    void handle(Update update, Replies replies) {
        if (update.hasCallbackQuery()) {
            var query = update.getCallbackQuery();
            String data = query.getData() == null ? "" : query.getData();
            if (data.startsWith(Onboarding.LANGUAGE_PREFIX)) {
                onboarding.onLanguageChosen(query).forEach(replies::execute);
            } else if (data.startsWith(Onboarding.LEVEL_PREFIX)) {
                onboarding.onLevelChosen(query).forEach(replies::execute);
            } else if (data.startsWith(MoreExamplesFlow.PREFIX)) {
                moreExamples.onMore(query, replies);
            } else if (data.startsWith(WordsListFlow.PREFIX)) {
                wordsList.onPage(query, replies);
            }
            return;
        }
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        var message = update.getMessage();
        if (isCommand(message.getText(), "start")) {
            onboarding.start(message).forEach(replies::execute);
        } else if (isCommand(message.getText(), "words")) {
            wordsList.onCommand(message, replies);
        } else if (!message.getText().strip().startsWith("/")) {
            wordFlow.onWord(message, replies);
        }
    }

    /**
     * Matches "/start", "/start payload" and "/start@this_bot", but not "/start@another_bot".
     */
    private boolean isCommand(String text, String command) {
        String first = text.strip().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        if (first.equals("/" + command)) {
            return true;
        }
        return !properties.username().isBlank()
                && first.equals("/" + command + "@" + properties.username().toLowerCase(Locale.ROOT));
    }
}
