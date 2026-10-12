package dev.words.bot;

import dev.words.application.profile.CreateProfile;
import dev.words.application.profile.ListProfiles;
import dev.words.application.profile.ProfileAlreadyExistsException;
import dev.words.application.profile.ProfileSummary;
import dev.words.application.user.RegisterUser;
import dev.words.domain.CefrLevel;
import dev.words.domain.Language;
import dev.words.domain.LearningProfile;
import dev.words.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UpdateHandlerTest {

    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");
    private static final long TELEGRAM_ID = 42;
    private static final long USER_ID = 7;

    private final RegisterUser registerUser = mock(RegisterUser.class);
    private final CreateProfile createProfile = mock(CreateProfile.class);
    private final ListProfiles listProfiles = mock(ListProfiles.class);
    private final WordFlow wordFlow = mock(WordFlow.class);
    private final UpdateHandler handler = new UpdateHandler(
            new TelegramProperties(true, "token", "words_test_bot"),
            new Onboarding(registerUser, createProfile, listProfiles), wordFlow);
    private final RecordingReplies replies = new RecordingReplies();

    @BeforeEach
    void setUp() {
        when(registerUser.register(anyLong())).thenReturn(new User(USER_ID, TELEGRAM_ID, null, NOW));
        when(listProfiles.list(USER_ID)).thenReturn(List.of());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/start", "  /START ", "/start deep-link-payload", "/start@words_test_bot"})
    void startRegistersNewUserAndOffersLanguages(String text) {
        handler.handle(Updates.text(TELEGRAM_ID, text), replies);

        verify(registerUser).register(TELEGRAM_ID);
        var reply = replies.get(0, SendMessage.class);
        assertThat(reply.getChatId()).isEqualTo("42");
        assertThat(reply.getText()).isEqualTo(Onboarding.WELCOME);
        assertThat(buttons(reply.getReplyMarkup())).extracting(InlineKeyboardButton::getCallbackData)
                .containsExactly("lang:en");
    }

    @Test
    void startForUserWithProfileSkipsOnboarding() {
        var profile = new LearningProfile(3L, USER_ID, Language.ENGLISH, Language.RUSSIAN, CefrLevel.B1, null, NOW);
        when(listProfiles.list(USER_ID)).thenReturn(List.of(new ProfileSummary(profile, 12, true)));

        handler.handle(Updates.text(TELEGRAM_ID, "/start"), replies);
        var reply = replies.get(0, SendMessage.class);

        assertThat(reply.getText()).contains("С возвращением", "английский", "B1");
        assertThat(reply.getReplyMarkup()).isNull();
    }

    @Test
    void choosingLanguageShowsLevels() {
        handler.handle(Updates.callback(TELEGRAM_ID, "lang:en"), replies);

        assertThat(replies.sent.get(0)).isInstanceOf(AnswerCallbackQuery.class);
        var edit = replies.get(1, EditMessageText.class);
        assertThat(edit.getMessageId()).isEqualTo(77);
        assertThat(edit.getText()).contains("английский", "уровень");
        assertThat(buttons(edit.getReplyMarkup())).extracting(InlineKeyboardButton::getCallbackData)
                .containsExactly("level:en:A1", "level:en:A2", "level:en:B1",
                        "level:en:B2", "level:en:C1", "level:en:C2");
    }

    @Test
    void choosingLevelCreatesEnglishProfileForRussianSpeaker() {
        handler.handle(Updates.callback(TELEGRAM_ID, "level:en:B2"), replies);

        verify(createProfile).create(USER_ID, Language.ENGLISH, Language.RUSSIAN, CefrLevel.B2);
        assertThat(replies.sent.get(0)).isInstanceOf(AnswerCallbackQuery.class);
        assertThat((replies.get(1, EditMessageText.class)).getText()).contains("Готово", "B2");
    }

    @Test
    void repeatedLevelTapIsHarmless() {
        when(createProfile.create(USER_ID, Language.ENGLISH, Language.RUSSIAN, CefrLevel.A1))
                .thenThrow(new ProfileAlreadyExistsException(Language.ENGLISH));

        handler.handle(Updates.callback(TELEGRAM_ID, "level:en:A1"), replies);

        assertThat((replies.get(1, EditMessageText.class)).getText()).contains("уже есть");
    }

    @Test
    void plainTextGoesToWordFlow() {
        var update = Updates.text(TELEGRAM_ID, "borrow");

        handler.handle(update, replies);

        verify(wordFlow).onWord(update.getMessage(), replies);
    }

    @Test
    void ignoresStartForAnotherBotUnknownCommandsCallbacksAndStickers() {
        handler.handle(Updates.text(TELEGRAM_ID, "/start@other_bot"), replies);
        handler.handle(Updates.text(TELEGRAM_ID, "/unknown"), replies);
        handler.handle(Updates.callback(TELEGRAM_ID, "unknown"), replies);
        handler.handle(Updates.sticker(TELEGRAM_ID), replies);

        assertThat(replies.sent).isEmpty();
        verifyNoInteractions(wordFlow);
    }

    private static List<InlineKeyboardButton> buttons(Object markup) {
        return ((InlineKeyboardMarkup) markup).getKeyboard().stream().flatMap(List::stream).toList();
    }
}
