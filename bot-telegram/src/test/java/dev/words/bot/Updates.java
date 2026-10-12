package dev.words.bot;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Builds updates the way Telegram sends them, from JSON.
 */
final class Updates {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Updates() {
    }

    static Update text(long chatId, String text) {
        try {
            return MAPPER.readValue("""
                    {"update_id": 1, "message": {"message_id": 10, "date": 1760000000,
                      "chat": {"id": %d, "type": "private"},
                      "from": {"id": %d, "is_bot": false, "first_name": "Stepan"},
                      "text": %s}}
                    """.formatted(chatId, chatId, MAPPER.writeValueAsString(text)), Update.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static Update callback(long userId, String data) {
        try {
            return MAPPER.readValue("""
                    {"update_id": 3, "callback_query": {"id": "cb-1", "chat_instance": "ci", "data": %s,
                      "from": {"id": %d, "is_bot": false, "first_name": "Stepan"},
                      "message": {"message_id": 77, "date": 1760000000,
                        "chat": {"id": %d, "type": "private"}, "text": "old"}}}
                    """.formatted(MAPPER.writeValueAsString(data), userId, userId), Update.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static Update sticker(long chatId) {
        try {
            return MAPPER.readValue("""
                    {"update_id": 2, "message": {"message_id": 11, "date": 1760000000,
                      "chat": {"id": %d, "type": "private"},
                      "sticker": {"file_id": "f", "file_unique_id": "u", "type": "regular",
                                  "width": 512, "height": 512, "is_animated": false, "is_video": false}}}
                    """.formatted(chatId), Update.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
