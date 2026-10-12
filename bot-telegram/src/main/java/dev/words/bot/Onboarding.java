package dev.words.bot;

import dev.words.application.profile.CreateProfile;
import dev.words.application.profile.ListProfiles;
import dev.words.application.profile.ProfileAlreadyExistsException;
import dev.words.application.profile.ProfileSummary;
import dev.words.application.user.RegisterUser;
import dev.words.domain.CefrLevel;
import dev.words.domain.Language;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.Arrays;
import java.util.List;

/**
 * /start: registers the user and, on first contact, asks for the language and level with inline buttons.
 * Callback data: {@code lang:<code>} after choosing the language, {@code level:<code>:<level>} after the level.
 */
@Component
class Onboarding {

    static final String LANGUAGE_PREFIX = "lang:";
    static final String LEVEL_PREFIX = "level:";

    /** MVP: English for Russian speakers. More languages come with /lang. */
    static final List<Language> LANGUAGES = List.of(Language.ENGLISH);
    static final Language NATIVE_LANGUAGE = Language.RUSSIAN;

    static final String WELCOME = """
            Привет! Я помогу запоминать иностранные слова.

            Пришли мне слово или фразу, и я покажу перевод, транскрипцию и простые примеры. \
            Слово попадёт в твою колоду, а я напомню его повторить.

            Какой язык учим?""";

    private final RegisterUser registerUser;
    private final CreateProfile createProfile;
    private final ListProfiles listProfiles;

    Onboarding(RegisterUser registerUser, CreateProfile createProfile, ListProfiles listProfiles) {
        this.registerUser = registerUser;
        this.createProfile = createProfile;
        this.listProfiles = listProfiles;
    }

    List<BotApiMethod<?>> start(Message message) {
        var user = registerUser.register(message.getFrom().getId());
        var active = listProfiles.list(user.id()).stream().filter(ProfileSummary::active).findFirst();
        if (active.isPresent()) {
            var profile = active.get().profile();
            return List.of(SendMessage.builder()
                    .chatId(message.getChatId())
                    .text("С возвращением! Учим %s, уровень %s. Пришли слово или фразу."
                            .formatted(languageName(profile.targetLanguage()), profile.level()))
                    .build());
        }
        return List.of(SendMessage.builder()
                .chatId(message.getChatId())
                .text(WELCOME)
                .replyMarkup(languageKeyboard())
                .build());
    }

    List<BotApiMethod<?>> onLanguageChosen(CallbackQuery query) {
        var language = Language.fromCode(query.getData().substring(LANGUAGE_PREFIX.length()));
        return List.of(
                answer(query),
                EditMessageText.builder()
                        .chatId(query.getMessage().getChatId())
                        .messageId(query.getMessage().getMessageId())
                        .text("Учим %s. Какой у тебя уровень?".formatted(languageName(language)))
                        .replyMarkup(levelKeyboard(language))
                        .build());
    }

    List<BotApiMethod<?>> onLevelChosen(CallbackQuery query) {
        String[] parts = query.getData().substring(LEVEL_PREFIX.length()).split(":");
        var language = Language.fromCode(parts[0]);
        var level = CefrLevel.parse(parts[1]);
        var user = registerUser.register(query.getFrom().getId());

        String text;
        try {
            createProfile.create(user.id(), language, NATIVE_LANGUAGE, level);
            text = """
                    Готово! Учим %s, уровень %s.

                    Пришли мне первое слово или фразу, например: borrow""".formatted(languageName(language), level);
        } catch (ProfileAlreadyExistsException e) {
            // A second tap on an old keyboard: the profile is already there.
            text = "%s уже есть в твоих языках. Пришли слово или фразу.".formatted(languageName(language));
        }
        return List.of(
                answer(query),
                EditMessageText.builder()
                        .chatId(query.getMessage().getChatId())
                        .messageId(query.getMessage().getMessageId())
                        .text(text)
                        .build());
    }

    static String languageName(Language language) {
        return switch (language) {
            case ENGLISH -> "английский";
            case RUSSIAN -> "русский";
        };
    }

    /** For phrases like "на английском языке". */
    static String languageNameAdjective(Language language) {
        return switch (language) {
            case ENGLISH -> "английском";
            case RUSSIAN -> "русском";
        };
    }

    private static AnswerCallbackQuery answer(CallbackQuery query) {
        return AnswerCallbackQuery.builder().callbackQueryId(query.getId()).build();
    }

    private static InlineKeyboardMarkup languageKeyboard() {
        var buttons = LANGUAGES.stream()
                .map(language -> button(flag(language) + " " + capitalize(languageName(language)),
                        LANGUAGE_PREFIX + language.code()))
                .toList();
        return InlineKeyboardMarkup.builder().keyboardRow(new InlineKeyboardRow(buttons)).build();
    }

    private static InlineKeyboardMarkup levelKeyboard(Language language) {
        var rows = Arrays.stream(CefrLevel.values())
                .map(level -> button(levelHint(level), LEVEL_PREFIX + language.code() + ":" + level))
                .toList();
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(rows.get(0), rows.get(1)))
                .keyboardRow(new InlineKeyboardRow(rows.get(2), rows.get(3)))
                .keyboardRow(new InlineKeyboardRow(rows.get(4), rows.get(5)))
                .build();
    }

    private static String levelHint(CefrLevel level) {
        return level + " · " + switch (level) {
            case A1 -> "начинаю";
            case A2 -> "знаю основы";
            case B1 -> "объясняюсь";
            case B2 -> "уверенно";
            case C1 -> "свободно";
            case C2 -> "как носитель";
        };
    }

    private static InlineKeyboardButton button(String text, String callbackData) {
        return InlineKeyboardButton.builder().text(text).callbackData(callbackData).build();
    }

    private static String flag(Language language) {
        return switch (language) {
            case ENGLISH -> "🇬🇧";
            case RUSSIAN -> "🇷🇺";
        };
    }

    private static String capitalize(String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
