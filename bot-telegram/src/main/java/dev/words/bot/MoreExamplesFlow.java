package dev.words.bot;

import dev.words.application.user.RegisterUser;
import dev.words.application.word.GetMoreExamples;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

/**
 * The "More examples" button. Callback data: {@code more:<lexemeId>:<offset>}, where offset is how many
 * examples the user has seen. Each tap sends a new message with the next examples and its own button,
 * and removes the button from the previous message so only the latest one is active.
 */
@Component
class MoreExamplesFlow {

    static final String PREFIX = "more:";
    static final String NO_MORE = "Больше примеров нет";

    private final RegisterUser registerUser;
    private final GetMoreExamples getMoreExamples;

    MoreExamplesFlow(RegisterUser registerUser, GetMoreExamples getMoreExamples) {
        this.registerUser = registerUser;
        this.getMoreExamples = getMoreExamples;
    }

    static InlineKeyboardMarkup button(long lexemeId, int offset) {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder()
                        .text("Ещё примеры")
                        .callbackData(PREFIX + lexemeId + ":" + offset)
                        .build()))
                .build();
    }

    void onMore(CallbackQuery query, Replies replies) {
        String[] parts = query.getData().substring(PREFIX.length()).split(":");
        long lexemeId = Long.parseLong(parts[0]);
        int offset = Integer.parseInt(parts[1]);
        var user = registerUser.register(query.getFrom().getId());
        long chatId = query.getMessage().getChatId();

        // Removing the old button first stops a second tap while examples are being generated.
        replies.execute(EditMessageReplyMarkup.builder()
                .chatId(chatId)
                .messageId(query.getMessage().getMessageId())
                .build());

        try {
            var page = getMoreExamples.next(user.id(), lexemeId, offset);
            if (page.examples().isEmpty()) {
                replies.execute(answer(query, NO_MORE));
                return;
            }
            replies.execute(answer(query, null));

            var text = new StringBuilder();
            for (int i = 0; i < page.examples().size(); i++) {
                if (i > 0) {
                    text.append('\n');
                }
                text.append(CardFormatter.example(offset + i + 1, page.examples().get(i), page.nativeLanguage()));
            }
            var message = SendMessage.builder().chatId(chatId).text(text.toString()).parseMode(ParseMode.HTML);
            if (page.hasMore()) {
                message.replyMarkup(button(lexemeId, page.nextOffset()));
            }
            replies.send(message.build());
        } catch (RuntimeException e) {
            replies.execute(answer(query, ErrorMessages.forUser(e)));
            // Give the button back so the user can try again.
            replies.execute(EditMessageReplyMarkup.builder()
                    .chatId(chatId)
                    .messageId(query.getMessage().getMessageId())
                    .replyMarkup(button(lexemeId, offset))
                    .build());
        }
    }

    private static AnswerCallbackQuery answer(CallbackQuery query, String text) {
        return AnswerCallbackQuery.builder().callbackQueryId(query.getId()).text(text).build();
    }
}
