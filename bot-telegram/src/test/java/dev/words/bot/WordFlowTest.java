package dev.words.bot;

import dev.words.application.port.LlmException;
import dev.words.application.user.RegisterUser;
import dev.words.application.word.AddWord;
import dev.words.application.word.AddedWord;
import dev.words.domain.CefrLevel;
import dev.words.domain.Language;
import dev.words.domain.LearningProfile;
import dev.words.domain.Lexeme;
import dev.words.domain.User;
import dev.words.domain.UserCard;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WordFlowTest {

    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");

    private final RegisterUser registerUser = mock(RegisterUser.class);
    private final AddWord addWord = mock(AddWord.class);
    private final WordFlow flow = new WordFlow(registerUser, addWord);
    private final RecordingReplies replies = new RecordingReplies();

    @Test
    void showsPlaceholderThenEditsItIntoCard() {
        when(registerUser.register(42)).thenReturn(new User(7L, 42, 3L, NOW));
        when(addWord.add(7L, "borrow")).thenReturn(borrow());

        flow.onWord(Updates.text(42, "borrow").getMessage(), replies);

        var placeholder = replies.get(0, SendMessage.class);
        assertThat(placeholder.getText()).isEqualTo(WordFlow.PREPARING);
        assertThat(placeholder.getReplyToMessageId()).isEqualTo(10);
        var card = replies.get(1, EditMessageText.class);
        assertThat(card.getMessageId()).isEqualTo(1000);
        assertThat(card.getParseMode()).isEqualTo(ParseMode.HTML);
        assertThat(card.getText()).startsWith("<b>borrow</b>").contains("брать взаймы");
        var button = ((InlineKeyboardMarkup) card.getReplyMarkup())
                .getKeyboard().getFirst().getFirst();
        assertThat(button.getText()).isEqualTo("Ещё примеры");
        assertThat(button.getCallbackData()).isEqualTo("more:1:0");
    }

    @Test
    void userWithoutLanguageIsSentToStart() {
        when(registerUser.register(42)).thenReturn(new User(7L, 42, null, NOW));

        flow.onWord(Updates.text(42, "borrow").getMessage(), replies);

        assertThat(replies.sent).singleElement()
                .satisfies(reply -> assertThat(((SendMessage) reply).getText()).isEqualTo(WordFlow.NO_PROFILE));
        verifyNoInteractions(addWord);
    }

    @Test
    void failureReplacesPlaceholderWithMessage() {
        when(registerUser.register(42)).thenReturn(new User(7L, 42, 3L, NOW));
        when(addWord.add(anyLong(), anyString())).thenThrow(new LlmException(LlmException.Reason.TIMEOUT, "slow"));

        flow.onWord(Updates.text(42, "borrow").getMessage(), replies);

        assertThat(replies.get(1, EditMessageText.class).getText()).startsWith("Карточка готовилась слишком долго");
        assertThat(replies.get(1, EditMessageText.class).getReplyMarkup()).isNull();
    }

    private static AddedWord borrow() {
        var profile = new LearningProfile(3L, 7L, Language.ENGLISH, Language.RUSSIAN, CefrLevel.A2, null, NOW);
        var lexeme = new Lexeme(1L, Language.ENGLISH, "borrow", "borrow", "verb", null, null,
                Map.of(Language.RUSSIAN, "брать взаймы"));
        return new AddedWord(profile, lexeme, List.of(), UserCard.newCard(3L, 1L, NOW), false);
    }
}
