package dev.words.bot;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.words.PostgresIntegrationTest;
import dev.words.application.port.UserRepository;
import dev.words.application.profile.ListProfiles;
import dev.words.domain.CefrLevel;
import dev.words.domain.Language;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.assertj.core.api.Assertions.assertThat;

class OnboardingIntegrationTest extends PostgresIntegrationTest {

    private static final long TELEGRAM_ID = 8001;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    UpdateHandler handler;

    @Autowired
    UserRepository users;

    @Autowired
    ListProfiles listProfiles;

    @Test
    void startThenLanguageThenLevelCreatesActiveProfile() throws Exception {
        handler.handle(update("""
                {"update_id": 1, "message": {"message_id": 1, "date": 1760000000,
                  "chat": {"id": 8001, "type": "private"}, "from": {"id": 8001, "is_bot": false, "first_name": "S"},
                  "text": "/start"}}"""));
        handler.handle(callback("lang:en"));
        handler.handle(callback("level:en:B1"));

        var user = users.findByTelegramId(TELEGRAM_ID).orElseThrow();
        var profiles = listProfiles.list(user.id());
        assertThat(profiles).singleElement().satisfies(summary -> {
            assertThat(summary.active()).isTrue();
            assertThat(summary.profile().targetLanguage()).isEqualTo(Language.ENGLISH);
            assertThat(summary.profile().nativeLanguage()).isEqualTo(Language.RUSSIAN);
            assertThat(summary.profile().level()).isEqualTo(CefrLevel.B1);
        });

        var again = (SendMessage) handler.handle(update("""
                {"update_id": 4, "message": {"message_id": 2, "date": 1760000000,
                  "chat": {"id": 8001, "type": "private"}, "from": {"id": 8001, "is_bot": false, "first_name": "S"},
                  "text": "/start"}}""")).getFirst();
        assertThat(again.getText()).startsWith("С возвращением");
    }

    private Update callback(String data) throws Exception {
        return update("""
                {"update_id": 2, "callback_query": {"id": "cb", "chat_instance": "ci", "data": "%s",
                  "from": {"id": 8001, "is_bot": false, "first_name": "S"},
                  "message": {"message_id": 1, "date": 1760000000, "chat": {"id": 8001, "type": "private"},
                    "text": "old"}}}""".formatted(data));
    }

    private Update update(String json) throws Exception {
        return mapper.readValue(json, Update.class);
    }
}
