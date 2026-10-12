package dev.words.bot;

import dev.words.application.error.NoActiveProfileException;
import dev.words.application.user.RegisterUser;
import dev.words.application.word.ListWords;
import dev.words.application.word.WordsPage;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;

/**
 * /words: the active profile's deck, newest words first, with ◀ ▶ buttons that flip pages in place.
 * Callback data: {@code words:<page>}.
 */
@Component
class WordsListFlow {

    static final String PREFIX = "words:";
    static final int PAGE_SIZE = 20;
    static final String EMPTY = "В колоде пока нет слов. Пришли слово или фразу, и я добавлю их.";

    private final RegisterUser registerUser;
    private final ListWords listWords;

    WordsListFlow(RegisterUser registerUser, ListWords listWords) {
        this.registerUser = registerUser;
        this.listWords = listWords;
    }

    void onCommand(Message message, Replies replies) {
        var user = registerUser.register(message.getFrom().getId());
        var reply = SendMessage.builder().chatId(message.getChatId()).parseMode(ParseMode.HTML);
        try {
            var page = listWords.list(user.id(), 0, PAGE_SIZE);
            reply.text(render(page)).replyMarkup(keyboard(page));
        } catch (NoActiveProfileException e) {
            reply.text(WordFlow.NO_PROFILE);
        }
        replies.send(reply.build());
    }

    void onPage(CallbackQuery query, Replies replies) {
        int requested = Integer.parseInt(query.getData().substring(PREFIX.length()));
        var user = registerUser.register(query.getFrom().getId());
        replies.execute(AnswerCallbackQuery.builder().callbackQueryId(query.getId()).build());

        var edit = EditMessageText.builder()
                .chatId(query.getMessage().getChatId())
                .messageId(query.getMessage().getMessageId())
                .parseMode(ParseMode.HTML);
        try {
            var page = listWords.list(user.id(), requested, PAGE_SIZE);
            edit.text(render(page)).replyMarkup(keyboard(page));
        } catch (NoActiveProfileException e) {
            edit.text(WordFlow.NO_PROFILE);
        }
        replies.execute(edit.build());
    }

    static String render(WordsPage page) {
        if (page.totalWords() == 0) {
            return EMPTY;
        }
        var nativeLanguage = page.profile().nativeLanguage();
        var text = new StringBuilder()
                .append("<b>Мои слова</b> · ")
                .append(Onboarding.languageName(page.profile().targetLanguage()))
                .append(" · ").append(page.totalWords()).append('\n');
        int number = page.page() * page.pageSize();
        for (var lexeme : page.words()) {
            text.append('\n').append(++number).append(". <b>").append(CardFormatter.escape(lexeme.text())).append("</b>");
            lexeme.meaningIn(nativeLanguage)
                    .ifPresent(meaning -> text.append(" — ").append(CardFormatter.escape(meaning)));
        }
        if (page.totalPages() > 1) {
            text.append("\n\nСтраница ").append(page.page() + 1).append(" из ").append(page.totalPages());
        }
        return text.toString();
    }

    private static InlineKeyboardMarkup keyboard(WordsPage page) {
        var buttons = new ArrayList<InlineKeyboardButton>();
        if (page.hasPrevious()) {
            buttons.add(button("◀", page.page() - 1));
        }
        if (page.hasNext()) {
            buttons.add(button("▶", page.page() + 1));
        }
        if (buttons.isEmpty()) {
            return null;
        }
        return InlineKeyboardMarkup.builder().keyboardRow(new InlineKeyboardRow(buttons)).build();
    }

    private static InlineKeyboardButton button(String text, int page) {
        return InlineKeyboardButton.builder().text(text).callbackData(PREFIX + page).build();
    }
}
