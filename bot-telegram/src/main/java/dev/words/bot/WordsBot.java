package dev.words.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Receives updates from long polling and answers each on its own virtual thread,
 * so a slow LLM call for one user does not hold up the others.
 */
class WordsBot implements LongPollingUpdateConsumer, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(WordsBot.class);

    private final UpdateHandler handler;
    private final Replies replies;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    WordsBot(TelegramClient telegram, UpdateHandler handler) {
        this.handler = handler;
        this.replies = new TelegramReplies(telegram);
    }

    @Override
    public void consume(List<Update> updates) {
        updates.forEach(update -> executor.submit(() -> process(update)));
    }

    void process(Update update) {
        try {
            handler.handle(update, replies);
        } catch (RuntimeException e) {
            log.error("Failed to process update {}", update.getUpdateId(), e);
        }
    }

    @Override
    public void close() {
        executor.close();
    }

    private record TelegramReplies(TelegramClient telegram) implements Replies {

        @Override
        public Message send(SendMessage message) {
            try {
                return telegram.execute(message);
            } catch (TelegramApiException e) {
                throw new TelegramSendException(e);
            }
        }

        @Override
        public void execute(BotApiMethod<?> method) {
            try {
                telegram.execute(method);
            } catch (TelegramApiException e) {
                throw new TelegramSendException(e);
            }
        }
    }

    static class TelegramSendException extends RuntimeException {

        TelegramSendException(TelegramApiException cause) {
            super(cause.getMessage(), cause);
        }
    }
}
