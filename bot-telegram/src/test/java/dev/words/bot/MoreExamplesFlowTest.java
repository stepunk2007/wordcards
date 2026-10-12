package dev.words.bot;

import dev.words.application.port.LlmException;
import dev.words.application.user.RegisterUser;
import dev.words.application.word.ExamplePage;
import dev.words.application.word.GetMoreExamples;
import dev.words.domain.CefrLevel;
import dev.words.domain.Example;
import dev.words.domain.Language;
import dev.words.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MoreExamplesFlowTest {

    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");

    private final RegisterUser registerUser = mock(RegisterUser.class);
    private final GetMoreExamples getMoreExamples = mock(GetMoreExamples.class);
    private final MoreExamplesFlow flow = new MoreExamplesFlow(registerUser, getMoreExamples);
    private final RecordingReplies replies = new RecordingReplies();

    @BeforeEach
    void setUp() {
        when(registerUser.register(42)).thenReturn(new User(7L, 42, 3L, NOW));
    }

    @Test
    void sendsNextExamplesWithNewButtonAndRemovesOldOne() {
        when(getMoreExamples.next(7L, 5L, 3)).thenReturn(new ExamplePage(
                List.of(example("Fourth."), example("Fifth."), example("Sixth.")), Language.RUSSIAN, 6, true));

        flow.onMore(Updates.callback(42, "more:5:3").getCallbackQuery(), replies);

        var removed = replies.get(0, EditMessageReplyMarkup.class);
        assertThat(removed.getMessageId()).isEqualTo(77);
        assertThat(removed.getReplyMarkup()).isNull();
        assertThat(replies.get(1, AnswerCallbackQuery.class).getText()).isNull();
        var message = replies.get(2, SendMessage.class);
        assertThat(message.getText()).isEqualTo("""
                4. Fourth.
                    <i>Перевод.</i>
                5. Fifth.
                    <i>Перевод.</i>
                6. Sixth.
                    <i>Перевод.</i>""");
        assertThat(((InlineKeyboardMarkup) message.getReplyMarkup()).getKeyboard().getFirst().getFirst()
                .getCallbackData()).isEqualTo("more:5:6");
    }

    @Test
    void lastPageHasNoButton() {
        when(getMoreExamples.next(7L, 5L, 27)).thenReturn(
                new ExamplePage(List.of(example("Last.")), Language.RUSSIAN, 28, false));

        flow.onMore(Updates.callback(42, "more:5:27").getCallbackQuery(), replies);

        assertThat(replies.get(2, SendMessage.class).getReplyMarkup()).isNull();
    }

    @Test
    void emptyPageTellsThereAreNoMore() {
        when(getMoreExamples.next(7L, 5L, 30)).thenReturn(new ExamplePage(List.of(), Language.RUSSIAN, 30, false));

        flow.onMore(Updates.callback(42, "more:5:30").getCallbackQuery(), replies);

        assertThat(replies.get(1, AnswerCallbackQuery.class).getText()).isEqualTo(MoreExamplesFlow.NO_MORE);
        assertThat(replies.sent).hasSize(2);
    }

    @Test
    void failureGivesTheButtonBack() {
        when(getMoreExamples.next(7L, 5L, 3)).thenThrow(new LlmException(LlmException.Reason.UNAVAILABLE, "down"));

        flow.onMore(Updates.callback(42, "more:5:3").getCallbackQuery(), replies);

        assertThat(replies.get(1, AnswerCallbackQuery.class).getText()).isEqualTo(MoreExamplesFlow.FAILED);
        var restored = replies.get(2, EditMessageReplyMarkup.class);
        assertThat(restored.getReplyMarkup().getKeyboard().getFirst().getFirst().getCallbackData())
                .isEqualTo("more:5:3");
    }

    private static Example example(String text) {
        return new Example(1L, 5L, text, CefrLevel.A2, Map.of(Language.RUSSIAN, "Перевод."));
    }
}
