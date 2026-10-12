package dev.words;

import dev.words.application.port.GeneratedExample;
import dev.words.application.port.GeneratedWordCard;
import dev.words.application.port.LlmPort;
import dev.words.application.profile.CreateProfile;
import dev.words.application.user.RegisterUser;
import dev.words.application.word.AddWord;
import dev.words.application.word.AddedWord;
import dev.words.domain.CefrLevel;
import dev.words.domain.Language;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AddWordIntegrationTest extends PostgresIntegrationTest {

    @MockitoBean
    LlmPort llm;

    @Autowired
    RegisterUser registerUser;

    @Autowired
    CreateProfile createProfile;

    @Autowired
    AddWord addWord;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void addsWordWithExamplesAndKeepsSingleCardOnRepeat() {
        when(llm.generateWordCard(any())).thenReturn(card("lend"));
        long userId = userWithProfile(7001);

        var first = addWord.add(userId, "Lent");
        var again = addWord.add(userId, "lend");

        assertThat(first.lexeme().normalizedText()).isEqualTo("lend");
        assertThat(first.examples()).hasSize(2);
        assertThat(again.alreadyInDeck()).isTrue();
        assertThat(again.card().id()).isEqualTo(first.card().id());
        assertThat(count("select count(*) from user_card where profile_id = ?", first.profile().id())).isOne();
    }

    @Test
    void concurrentFirstAddsOfNewWordCreateSingleLexeme() throws Exception {
        when(llm.generateWordCard(any())).thenReturn(card("persuade"));
        var userIds = IntStream.range(0, 6).mapToLong(i -> userWithProfile(7100 + i)).boxed().toList();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var tasks = userIds.stream().<Callable<AddedWord>>map(id -> () -> addWord.add(id, "persuade")).toList();
            for (Future<AddedWord> result : executor.invokeAll(tasks)) {
                assertThat(result.get().lexeme().normalizedText()).isEqualTo("persuade");
            }
        }

        assertThat(count("select count(*) from lexeme where normalized_text = 'persuade'")).isOne();
        assertThat(count("""
                select count(*) from example e join lexeme l on l.id = e.lexeme_id
                where l.normalized_text = 'persuade'""")).isEqualTo(2);
        assertThat(count("""
                select count(*) from user_card c join lexeme l on l.id = c.lexeme_id
                where l.normalized_text = 'persuade'""")).isEqualTo(6);
    }

    private long userWithProfile(long telegramId) {
        long userId = registerUser.register(telegramId).id();
        createProfile.create(userId, Language.ENGLISH, Language.RUSSIAN, CefrLevel.A2);
        return userId;
    }

    private int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    private static GeneratedWordCard card(String lemma) {
        return new GeneratedWordCard(lemma, "verb", null, "перевод", List.of(
                new GeneratedExample("Example one with " + lemma + ".", "Пример один.", CefrLevel.A1),
                new GeneratedExample("Example two with " + lemma + ".", "Пример два.", CefrLevel.A2)));
    }
}
