package dev.words;

import dev.words.application.port.GeneratedExample;
import dev.words.application.port.GeneratedWordCard;
import dev.words.application.port.LlmPort;
import dev.words.application.port.ReviewLogRepository;
import dev.words.application.port.UserCardRepository;
import dev.words.application.profile.CreateProfile;
import dev.words.application.review.GetDueCards;
import dev.words.application.review.ReviewCard;
import dev.words.application.user.RegisterUser;
import dev.words.application.word.AddWord;
import dev.words.domain.CardState;
import dev.words.domain.CefrLevel;
import dev.words.domain.Grade;
import dev.words.domain.Language;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ReviewIntegrationTest extends PostgresIntegrationTest {

    @MockitoBean
    LlmPort llm;

    @Autowired
    RegisterUser registerUser;

    @Autowired
    CreateProfile createProfile;

    @Autowired
    AddWord addWord;

    @Autowired
    GetDueCards getDueCards;

    @Autowired
    ReviewCard reviewCard;

    @Autowired
    UserCardRepository cards;

    @Autowired
    ReviewLogRepository reviewLogs;

    @Test
    void newWordIsDueAndReviewIsStoredWithFsrsState() {
        when(llm.generateWordCard(any())).thenReturn(new GeneratedWordCard("ponder", "verb", null, "обдумывать",
                List.of(new GeneratedExample("I pondered the question.", "Я обдумал вопрос.", CefrLevel.B1))));
        long userId = registerUser.register(9001).id();
        createProfile.create(userId, Language.ENGLISH, Language.RUSSIAN, CefrLevel.B1);
        long cardId = addWord.add(userId, "ponder").card().id();

        var due = getDueCards.due(userId, 10);
        assertThat(due.totalDue()).isOne();
        assertThat(due.cards()).singleElement()
                .satisfies(card -> assertThat(card.lexeme().text()).isEqualTo("ponder"));

        var reviewed = reviewCard.review(userId, cardId, Grade.GOOD);

        var stored = cards.findById(cardId).orElseThrow();
        assertThat(stored.state()).isEqualTo(CardState.LEARNING);
        assertThat(stored.step()).isOne();
        assertThat(stored.lastReviewedAt()).isNotNull();
        assertThat(stored.stability()).isEqualTo(reviewed.stability());
        assertThat(stored.dueAt()).isEqualTo(reviewed.dueAt());
        assertThat(reviewLogs.findByUserCardId(cardId)).singleElement()
                .satisfies(log -> assertThat(log.grade()).isEqualTo(Grade.GOOD));
        assertThat(getDueCards.due(userId, 10).totalDue()).isZero();
    }
}
