package dev.words.bot;

import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;

/**
 * Sends answers to Telegram. Flows that need the id of a sent message (to edit it later) use {@link #send};
 * tests replace it with a recorder.
 */
interface Replies {

    Message send(SendMessage message);

    void execute(BotApiMethod<?> method);
}
