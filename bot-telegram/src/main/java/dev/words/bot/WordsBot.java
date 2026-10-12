package dev.words.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
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

    private final TelegramClient telegram;
    private final UpdateHandler handler;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    WordsBot(TelegramClient telegram, UpdateHandler handler) {
        this.telegram = telegram;
        this.handler = handler;
    }

    @Override
    public void consume(List<Update> updates) {
        updates.forEach(update -> executor.submit(() -> process(update)));
    }

    void process(Update update) {
        try {
            for (var reply : handler.handle(update)) {
                telegram.execute(reply);
            }
        } catch (TelegramApiException | RuntimeException e) {
            log.error("Failed to process update {}", update.getUpdateId(), e);
        }
    }

    @Override
    public void close() {
        executor.close();
    }
}
