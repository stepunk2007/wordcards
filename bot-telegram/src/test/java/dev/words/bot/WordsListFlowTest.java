package dev.words.bot;

import dev.words.application.error.NoActiveProfileException;
import dev.words.application.user.RegisterUser;
import dev.words.application.word.ListWords;
import dev.words.application.word.WordsPage;
import dev.words.domain.CefrLevel;
import dev.words.domain.Language;
import dev.words.domain.LearningProfile;
import dev.words.domain.Lexeme;
import dev.words.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WordsListFlowTest {

    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");
    private static final LearningProfile PROFILE =
            new LearningProfile(3L, 7L, Language.ENGLISH, Language.RUSSIAN, CefrLevel.A2, null, NOW);

    private final RegisterUser registerUser = mock(RegisterUser.class);
    private final ListWords listWords = mock(ListWords.class);
    private final WordsListFlow flow = new WordsListFlow(registerUser, listWords);
    private final RecordingReplies replies = new RecordingReplies();

    @BeforeEach
    void setUp() {
        when(registerUser.register(42)).thenReturn(new User(7L, 42, 3L, NOW));
    }

    @Test
    void firstPageListsWordsWithMeaningsAndNextButton() {
        when(listWords.list(7L, 0, WordsListFlow.PAGE_SIZE)).thenReturn(page(0, 45, "borrow", "look up"));

        flow.onCommand(Updates.text(42, "/words").getMessage(), replies);

        var message = replies.get(0, SendMessage.class);
        assertThat(message.getText()).isEqualTo("""
                <b>Мои слова</b> · английский · 45

                1. <b>borrow</b> — перевод borrow
                2. <b>look up</b> — перевод look up

                Страница 1 из 3""");
        assertThat(buttons(message.getReplyMarkup())).extracting(InlineKeyboardButton::getCallbackData)
                .containsExactly("words:1");
    }

    @Test
    void pageButtonEditsMessageInPlaceWithBothArrows() {
        when(listWords.list(7L, 1, WordsListFlow.PAGE_SIZE)).thenReturn(page(1, 45, "cherish"));

        flow.onPage(Updates.callback(42, "words:1").getCallbackQuery(), replies);

        assertThat(replies.sent.get(0)).isInstanceOf(AnswerCallbackQuery.class);
        var edit = replies.get(1, EditMessageText.class);
        assertThat(edit.getMessageId()).isEqualTo(77);
        assertThat(edit.getText()).contains("21. <b>cherish</b>", "Страница 2 из 3");
        assertThat(buttons(edit.getReplyMarkup())).extracting(InlineKeyboardButton::getCallbackData)
                .containsExactly("words:0", "words:2");
    }

    @Test
    void singlePageHasNoButtonsAndNoPageCounter() {
        when(listWords.list(7L, 0, WordsListFlow.PAGE_SIZE)).thenReturn(page(0, 1, "borrow"));

        flow.onCommand(Updates.text(42, "/words").getMessage(), replies);

        var message = replies.get(0, SendMessage.class);
        assertThat(message.getText()).doesNotContain("Страница");
        assertThat(message.getReplyMarkup()).isNull();
    }

    @Test
    void emptyDeckSuggestsAddingWords() {
        when(listWords.list(7L, 0, WordsListFlow.PAGE_SIZE)).thenReturn(page(0, 0));

        flow.onCommand(Updates.text(42, "/words").getMessage(), replies);

        assertThat(replies.get(0, SendMessage.class).getText()).isEqualTo(WordsListFlow.EMPTY);
    }

    @Test
    void userWithoutLanguageIsSentToStart() {
        when(listWords.list(7L, 0, WordsListFlow.PAGE_SIZE)).thenThrow(new NoActiveProfileException(7L));

        flow.onCommand(Updates.text(42, "/words").getMessage(), replies);

        assertThat(replies.get(0, SendMessage.class).getText()).isEqualTo(WordFlow.NO_PROFILE);
    }

    @Test
    void escapesHtmlInWords() {
        assertThat(WordsListFlow.render(page(0, 1, "R&D"))).contains("<b>R&amp;D</b> — перевод R&amp;D");
    }

    private static WordsPage page(int page, long total, String... words) {
        List<Lexeme> lexemes = IntStream.range(0, words.length)
                .mapToObj(i -> new Lexeme((long) i, Language.ENGLISH, words[i], words[i], null, null, null,
                        Map.of(Language.RUSSIAN, "перевод " + words[i])))
                .toList();
        return new WordsPage(PROFILE, lexemes, page, WordsListFlow.PAGE_SIZE, total);
    }

    private static List<InlineKeyboardButton> buttons(Object markup) {
        return ((InlineKeyboardMarkup) markup).getKeyboard().stream().flatMap(List::stream).toList();
    }
}
