package dev.words.bot;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.words.PostgresIntegrationTest;
import dev.words.application.port.GeneratedExample;
import dev.words.application.port.GeneratedWordCard;
import dev.words.application.port.LlmPort;
import dev.words.application.port.UserRepository;
import dev.words.application.profile.ListProfiles;
import dev.words.domain.CefrLevel;
import dev.words.domain.Language;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * The bot conversation end to end against a real database, with the LLM stubbed.
 */
class OnboardingIntegrationTest extends PostgresIntegrationTest {

    private static final long TELEGRAM_ID = 8001;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Recorder replies = new Recorder();

    @MockitoBean
    LlmPort llm;

    @Autowired
    UpdateHandler handler;

    @Autowired
    UserRepository users;

    @Autowired
    ListProfiles listProfiles;

    @Test
    void startChooseLanguageAndLevelThenAddWord() throws Exception {
        handler.handle(text("/start"), replies);
        handler.handle(callback("lang:en"), replies);
        handler.handle(callback("level:en:B1"), replies);

        var user = users.findByTelegramId(TELEGRAM_ID).orElseThrow();
        assertThat(listProfiles.list(user.id())).singleElement().satisfies(summary -> {
            assertThat(summary.active()).isTrue();
            assertThat(summary.profile().targetLanguage()).isEqualTo(Language.ENGLISH);
            assertThat(summary.profile().nativeLanguage()).isEqualTo(Language.RUSSIAN);
            assertThat(summary.profile().level()).isEqualTo(CefrLevel.B1);
        });

        when(llm.generateWordCard(any())).thenReturn(new GeneratedWordCard("cherish", "verb", "ˈtʃɛrɪʃ",
                "дорожить", List.of(new GeneratedExample("I cherish our friendship.", "Я дорожу нашей дружбой.",
                        CefrLevel.B1))));
        replies.sent.clear();
        handler.handle(text("Cherish"), replies);

        assertThat(((SendMessage) replies.sent.get(0)).getText()).isEqualTo(WordFlow.PREPARING);
        assertThat(((EditMessageText) replies.sent.get(1)).getText())
                .startsWith("<b>cherish</b>  /ˈtʃɛrɪʃ/")
                .contains("дорожить", "I cherish our friendship.", "Я дорожу нашей дружбой.");

        replies.sent.clear();
        handler.handle(text("/start"), replies);
        assertThat(((SendMessage) replies.sent.getFirst()).getText()).startsWith("С возвращением");
    }

    private Update text(String text) throws Exception {
        return mapper.readValue("""
                {"update_id": 1, "message": {"message_id": 1, "date": 1760000000,
                  "chat": {"id": 8001, "type": "private"}, "from": {"id": 8001, "is_bot": false, "first_name": "S"},
                  "text": "%s"}}""".formatted(text), Update.class);
    }

    private Update callback(String data) throws Exception {
        return mapper.readValue("""
                {"update_id": 2, "callback_query": {"id": "cb", "chat_instance": "ci", "data": "%s",
                  "from": {"id": 8001, "is_bot": false, "first_name": "S"},
                  "message": {"message_id": 1, "date": 1760000000, "chat": {"id": 8001, "type": "private"},
                    "text": "old"}}}""".formatted(data), Update.class);
    }

    private class Recorder implements Replies {

        final List<Object> sent = new ArrayList<>();

        @Override
        public Message send(SendMessage message) {
            sent.add(message);
            try {
                return mapper.readValue("""
                        {"message_id": 500, "date": 1760000000, "chat": {"id": 8001, "type": "private"}}
                        """, Message.class);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void execute(BotApiMethod<?> method) {
            sent.add(method);
        }
    }
}
