package dev.words.bot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Locale;

/**
 * Routes an update to the flow that answers it. Has no Telegram I/O, so it is tested without a network.
 */
@Component
class UpdateHandler {

    private final TelegramProperties properties;
    private final Onboarding onboarding;

    UpdateHandler(TelegramProperties properties, Onboarding onboarding) {
        this.properties = properties;
        this.onboarding = onboarding;
    }

    List<BotApiMethod<?>> handle(Update update) {
        if (update.hasCallbackQuery()) {
            var query = update.getCallbackQuery();
            String data = query.getData() == null ? "" : query.getData();
            if (data.startsWith(Onboarding.LANGUAGE_PREFIX)) {
                return onboarding.onLanguageChosen(query);
            }
            if (data.startsWith(Onboarding.LEVEL_PREFIX)) {
                return onboarding.onLevelChosen(query);
            }
            return List.of();
        }
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return List.of();
        }
        var message = update.getMessage();
        if (isCommand(message.getText(), "start")) {
            return onboarding.start(message);
        }
        return List.of();
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
