package dev.words.bot;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WordsBotTest {

    private final TelegramClient telegram = mock(TelegramClient.class);
    private final UpdateHandler handler = mock(UpdateHandler.class);
    private final WordsBot bot = new WordsBot(telegram, handler);

    @Test
    void handlerRepliesGoToTelegram() throws TelegramApiException {
        doAnswer(invocation -> {
            invocation.<Replies>getArgument(1).execute(SendMessage.builder().chatId(42L).text("hi").build());
            return null;
        }).when(handler).handle(any(), any());

        bot.process(Updates.text(42, "/start"));

        verify(telegram).execute(any(SendMessage.class));
    }

    @Test
    void telegramErrorDoesNotEscape() throws TelegramApiException {
        when(telegram.execute(any(SendMessage.class))).thenThrow(new TelegramApiException("chat not found"));
        doAnswer(invocation -> {
            invocation.<Replies>getArgument(1).execute(SendMessage.builder().chatId(42L).text("hi").build());
            return null;
        }).when(handler).handle(any(), any());

        assertThatCode(() -> bot.process(Updates.text(42, "/start"))).doesNotThrowAnyException();
    }
}
