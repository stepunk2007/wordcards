package dev.words.bot;

import dev.words.application.error.NoActiveProfileException;
import dev.words.application.port.LlmException;
import dev.words.application.port.WordNotInLanguageException;
import dev.words.application.word.DailyLimitExceededException;
import dev.words.application.word.InvalidWordException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Turns a failure of a use case into a message the user can act on; the text is safe to send as HTML.
 * Failures that only the operator can fix are logged as errors; the rest are expected and logged quietly.
 */
final class ErrorMessages {

    static final String GENERIC = "Что-то пошло не так. Попробуй ещё раз чуть позже.";

    private static final Logger log = LoggerFactory.getLogger(ErrorMessages.class);

    private ErrorMessages() {
    }

    static String forUser(RuntimeException error) {
        return switch (error) {
            case InvalidWordException _ -> "Пришли слово или короткую фразу, до 100 символов.";
            case NoActiveProfileException _ -> WordFlow.NO_PROFILE;
            case DailyLimitExceededException e -> """
                    На сегодня новых слов достаточно: дневной лимит %d. \
                    Завтра можно будет добавить ещё, а слова из колоды доступны и сейчас: /words"""
                    .formatted(e.limit());
            case WordNotInLanguageException e -> """
                    «%s» не похоже на слово или фразу на %s языке. \
                    Проверь написание или пришли другое слово.""".formatted(
                    CardFormatter.escape(e.text()), Onboarding.languageNameAdjective(e.expectedLanguage()));
            case LlmException e -> forLlm(e);
            default -> {
                log.error("Unexpected error while answering the user", error);
                yield GENERIC;
            }
        };
    }

    private static String forLlm(LlmException e) {
        log.warn("LLM failed: {} ({})", e.getMessage(), e.reason());
        return switch (e.reason()) {
            case TIMEOUT -> "Карточка готовилась слишком долго. Попробуй ещё раз через минуту.";
            case UNAVAILABLE -> "Сервис, который составляет карточки, сейчас перегружен. Попробуй через пару минут.";
            case INVALID_RESPONSE -> "Не получилось составить карточку. Попробуй ещё раз.";
            case REFUSED -> "Для этого слова карточку составить не получится. Попробуй другое.";
            case MISCONFIGURED -> {
                log.error("LLM is misconfigured, new cards cannot be generated", e);
                yield "Бот временно не может составлять новые карточки. Слова из колоды доступны: /words";
            }
        };
    }
}
