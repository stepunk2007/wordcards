package dev.words.bot;

import dev.words.application.user.RegisterUser;
import dev.words.application.word.AddWord;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

/**
 * A plain text message is a word to learn: answer "Preparing the card…" at once, since generation takes seconds,
 * then edit that message into the finished card.
 */
@Component
class WordFlow {

    static final String PREPARING = "Готовлю карточку…";
    static final String NO_PROFILE = "Сначала выбери язык, который учишь: /start";

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
        InlineKeyboardMarkup keyboard = null;
        try {
            var added = addWord.add(user.id(), message.getText());
            text = CardFormatter.card(added);
            keyboard = MoreExamplesFlow.button(added.lexeme().id(),
                    Math.min(CardFormatter.EXAMPLES_ON_CARD, added.examples().size()));
        } catch (RuntimeException e) {
            text = ErrorMessages.forUser(e);
        }

        replies.execute(EditMessageText.builder()
                .chatId(message.getChatId())
                .messageId(placeholder.getMessageId())
                .text(text)
                .parseMode(ParseMode.HTML)
                .replyMarkup(keyboard)
                .build());
    }
}
