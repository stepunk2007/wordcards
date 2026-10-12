package dev.words.bot;

import dev.words.application.error.NoActiveProfileException;
import dev.words.application.user.RegisterUser;
import dev.words.application.word.AddWord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.message.Message;

/**
 * A plain text message is a word to learn: answer "Preparing the card…" at once, since generation takes seconds,
 * then edit that message into the finished card.
 */
@Component
class WordFlow {

    static final String PREPARING = "Готовлю карточку…";
    static final String NO_PROFILE = "Сначала выбери язык, который учишь: /start";
    static final String FAILED = "Не получилось подготовить карточку. Попробуй ещё раз чуть позже.";

    private static final Logger log = LoggerFactory.getLogger(WordFlow.class);

    private final RegisterUser registerUser;
    private final AddWord addWord;

    WordFlow(RegisterUser registerUser, AddWord addWord) {
        this.registerUser = registerUser;
        this.addWord = addWord;
    }

    void onWord(Message message, Replies replies) {
        var user = registerUser.register(message.getFrom().getId());
        if (!user.hasActiveProfile()) {
            replies.send(SendMessage.builder().chatId(message.getChatId()).text(NO_PROFILE).build());
            return;
        }

        var placeholder = replies.send(SendMessage.builder()
                .chatId(message.getChatId())
                .text(PREPARING)
                .replyToMessageId(message.getMessageId())
                .build());

        String text;
        try {
            text = CardFormatter.card(addWord.add(user.id(), message.getText()));
        } catch (NoActiveProfileException e) {
            text = NO_PROFILE;
        } catch (RuntimeException e) {
            // Specific messages for each failure come in task 7.6.
            log.warn("Could not add word '{}' for user {}", message.getText(), user.id(), e);
            text = FAILED;
        }

        replies.execute(EditMessageText.builder()
                .chatId(message.getChatId())
                .messageId(placeholder.getMessageId())
                .text(text)
                .parseMode(ParseMode.HTML)
                .build());
    }
}
