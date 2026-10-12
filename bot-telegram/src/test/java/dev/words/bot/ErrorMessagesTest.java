package dev.words.bot;

import dev.words.application.error.NoActiveProfileException;
import dev.words.application.port.LlmException;
import dev.words.application.port.LlmException.Reason;
import dev.words.application.port.WordNotInLanguageException;
import dev.words.application.word.DailyLimitExceededException;
import dev.words.application.word.InvalidWordException;
import dev.words.domain.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorMessagesTest {

    @Test
    void dailyLimitNamesTheLimitAndPointsToTheDeck() {
        assertThat(ErrorMessages.forUser(new DailyLimitExceededException(20)))
                .contains("дневной лимит 20", "/words");
    }

    @Test
    void wordInAnotherLanguageQuotesItEscaped() {
        assertThat(ErrorMessages.forUser(new WordNotInLanguageException("книга <b>", Language.ENGLISH)))
                .startsWith("«книга &lt;b&gt;» не похоже на слово или фразу на английском языке");
    }

    @Test
    void invalidWordAndMissingProfile() {
        assertThat(ErrorMessages.forUser(new InvalidWordException("too long"))).contains("до 100 символов");
        assertThat(ErrorMessages.forUser(new NoActiveProfileException(7))).isEqualTo(WordFlow.NO_PROFILE);
    }

    @ParameterizedTest
    @EnumSource(Reason.class)
    void everyLlmFailureHasItsOwnShortMessage(Reason reason) {
        String text = ErrorMessages.forUser(new LlmException(reason, "test"));

        assertThat(text).isNotEqualTo(ErrorMessages.GENERIC);
        // Also used as a callback toast, which Telegram cuts at 200 characters.
        assertThat(text.length()).isLessThanOrEqualTo(200);
    }

    @Test
    void retryableLlmFailuresSuggestTryingAgain() {
        assertThat(ErrorMessages.forUser(new LlmException(Reason.TIMEOUT, "slow"))).contains("ещё раз");
        assertThat(ErrorMessages.forUser(new LlmException(Reason.UNAVAILABLE, "down"))).contains("Попробуй");
    }

    @Test
    void unexpectedErrorGetsGenericMessage() {
        assertThat(ErrorMessages.forUser(new IllegalStateException("boom"))).isEqualTo(ErrorMessages.GENERIC);
    }
}
