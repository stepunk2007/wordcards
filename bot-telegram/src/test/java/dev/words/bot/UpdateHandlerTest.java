package dev.words.bot;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateHandlerTest {

    private final UpdateHandler handler = new UpdateHandler(new TelegramProperties(true, "token", "words_test_bot"));

    @ParameterizedTest
    @ValueSource(strings = {"/start", "  /START ", "/start deep-link-payload", "/start@words_test_bot"})
    void answersStartWithWelcome(String text) {
        var replies = handler.handle(Updates.text(42, text));

        assertThat(replies).singleElement().satisfies(reply -> {
            assertThat(reply.getChatId()).isEqualTo("42");
            assertThat(reply.getText()).isEqualTo(UpdateHandler.WELCOME);
        });
    }

    @Test
    void ignoresStartAddressedToAnotherBot() {
        assertThat(handler.handle(Updates.text(42, "/start@other_bot"))).isEmpty();
    }

    @Test
    void ignoresOtherTextAndNonTextMessagesForNow() {
        assertThat(handler.handle(Updates.text(42, "borrow"))).isEmpty();
        assertThat(handler.handle(Updates.sticker(42))).isEmpty();
    }
}
