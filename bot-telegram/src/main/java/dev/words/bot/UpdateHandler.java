package dev.words.bot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Locale;

/**
 * Decides how the bot answers an update. Has no Telegram I/O, so it is tested without a network.
 */
@Component
class UpdateHandler {

    static final String WELCOME = """
            Привет! Я помогу запоминать иностранные слова.

            Пришли мне слово или фразу, и я покажу перевод, транскрипцию и простые примеры. \
            Слово попадёт в твою колоду, а я напомню его повторить.""";

    private final TelegramProperties properties;

    UpdateHandler(TelegramProperties properties) {
        this.properties = properties;
    }

    List<SendMessage> handle(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return List.of();
        }
        var message = update.getMessage();
        if (isCommand(message.getText(), "start")) {
            return List.of(SendMessage.builder().chatId(message.getChatId()).text(WELCOME).build());
        }
        return List.of();
    }

    /**
     * Matches "/start", "/start payload" and "/start@this_bot", but not "/start@another_bot".
     */
    private boolean isCommand(String text, String command) {
        String first = text.strip().split("\s+", 2)[0].toLowerCase(Locale.ROOT);
        if (first.equals("/" + command)) {
            return true;
        }
        return !properties.username().isBlank()
                && first.equals("/" + command + "@" + properties.username().toLowerCase(Locale.ROOT));
    }
}
