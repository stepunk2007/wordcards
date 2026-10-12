package dev.words.bot;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects everything the bot would send; sent messages get ids 1000, 1001, ...
 */
class RecordingReplies implements Replies {

    final List<Object> sent = new ArrayList<>();
    private int nextMessageId = 1000;

    @Override
    public Message send(SendMessage message) {
        sent.add(message);
        try {
            return new ObjectMapper().readValue("""
                    {"message_id": %d, "date": 1760000000, "chat": {"id": %s, "type": "private"}}
                    """.formatted(nextMessageId++, message.getChatId()), Message.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void execute(BotApiMethod<?> method) {
        sent.add(method);
    }

    <T> T get(int index, Class<T> type) {
        return type.cast(sent.get(index));
    }
}
