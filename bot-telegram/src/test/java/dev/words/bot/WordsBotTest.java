package dev.words.bot;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WordsBotTest {

    private final TelegramClient telegram = mock(TelegramClient.class);
    private final WordsBot bot =
            new WordsBot(telegram, new UpdateHandler(new TelegramProperties(true, "token", "words_test_bot")));

    @Test
    void sendsReplyForStart() throws TelegramApiException {
        bot.process(Updates.text(42, "/start"));

        verify(telegram).execute(any(SendMessage.class));
    }

    @Test
    void sendsNothingWhenThereIsNoReply() throws TelegramApiException {
        bot.process(Updates.text(42, "hello"));

        verify(telegram, never()).execute(any(SendMessage.class));
    }

    @Test
    void telegramErrorDoesNotEscape() throws TelegramApiException {
        when(telegram.execute(any(SendMessage.class))).thenThrow(new TelegramApiException("chat not found"));

        assertThatCode(() -> bot.process(Updates.text(42, "/start"))).doesNotThrowAnyException();
    }
}
