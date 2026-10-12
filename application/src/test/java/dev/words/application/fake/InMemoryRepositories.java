package dev.words.application.fake;

import dev.words.application.port.ExampleRepository;
import dev.words.application.port.LearningProfileRepository;
import dev.words.application.port.LexemeRepository;
import dev.words.domain.Example;
import dev.words.domain.Lexeme;
import dev.words.application.port.UserCardRepository;
import dev.words.application.port.UserRepository;
import dev.words.domain.Language;
import dev.words.domain.LearningProfile;
import dev.words.domain.User;
import dev.words.domain.UserCard;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryRepositories {

    public static class Users implements UserRepository {

        public final Map<Long, User> byId = new LinkedHashMap<>();
        public int saves;
        private long nextId = 1;

        @Override
        public User save(User user) {
            saves++;
            long id = user.id() != null ? user.id() : nextId++;
            var saved = new User(id, user.telegramId(), user.activeProfileId(), user.createdAt());
            byId.put(id, saved);
            return saved;
        }

        @Override
        public Optional<User> findById(long id) {
            return Optional.ofNullable(byId.get(id))
                    .map(user -> new User(user.id(), user.telegramId(), user.activeProfileId(), user.createdAt()));
        }

        @Override
        public Optional<User> findByTelegramId(long telegramId) {
            return byId.values().stream().filter(user -> user.telegramId() == telegramId).findFirst();
        }
    }

    public static class Lexemes implements LexemeRepository {

        public final Map<Long, Lexeme> byId = new LinkedHashMap<>();
        private long nextId = 500;

        @Override
        public Lexeme save(Lexeme lexeme) {
            boolean duplicate = byId.values().stream().anyMatch(existing -> !existing.id().equals(lexeme.id())
                    && existing.language() == lexeme.language()
                    && existing.normalizedText().equals(lexeme.normalizedText()));
            if (duplicate) {
                throw new IllegalStateException("duplicate lexeme " + lexeme.normalizedText());
            }
            long id = lexeme.id() != null ? lexeme.id() : nextId++;
            var saved = new Lexeme(id, lexeme.language(), lexeme.text(), lexeme.normalizedText(),
                    lexeme.partOfSpeech(), lexeme.ipa(), lexeme.ipaSource(), lexeme.meanings());
            byId.put(id, saved);
            return saved;
        }

        @Override
        public Optional<Lexeme> findById(long id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public List<Lexeme> findAllById(Collection<Long> ids) {
            return byId.values().stream().filter(lexeme -> ids.contains(lexeme.id())).toList();
        }

        @Override
        public Optional<Lexeme> findByLanguageAndNormalizedText(Language language, String normalizedText) {
            return byId.values().stream()
                    .filter(l -> l.language() == language && l.normalizedText().equals(normalizedText))
                    .findFirst();
        }
    }

    public static class Examples implements ExampleRepository {

        public final List<Example> all = new ArrayList<>();
        private long nextId = 5000;

        @Override
        public List<Example> saveAll(List<Example> examples) {
            var saved = examples.stream()
                    .map(e -> new Example(nextId++, e.lexemeId(), e.text(), e.level(), e.translations()))
                    .toList();
            all.addAll(saved);
            return saved;
        }

        @Override
        public List<Example> findByLexemeId(long lexemeId) {
            return all.stream().filter(e -> e.lexemeId() == lexemeId).toList();
        }
    }

    public static class Cards implements UserCardRepository {

        public final List<UserCard> all = new ArrayList<>();
        private long nextId = 1000;

        @Override
        public UserCard save(UserCard card) {
            var saved = new UserCard(card.id() != null ? card.id() : nextId++, card.profileId(), card.lexemeId(),
                    card.state(), card.dueAt(), card.stability(), card.difficulty(), card.reps(), card.lapses(),
                    card.step(), card.lastReviewedAt(), card.createdAt());
            all.removeIf(existing -> existing.id().equals(saved.id()));
            all.add(saved);
            return saved;
        }

        @Override
        public Optional<UserCard> findById(long id) {
            return all.stream().filter(card -> card.id() == id).findFirst();
        }

        @Override
        public Optional<UserCard> findByProfileIdAndLexemeId(long profileId, long lexemeId) {
            return all.stream()
                    .filter(card -> card.profileId() == profileId && card.lexemeId() == lexemeId)
                    .findFirst();
        }

        @Override
        public List<UserCard> findDue(long profileId, Instant now, int limit) {
            return all.stream()
                    .filter(card -> card.profileId() == profileId && card.isDue(now))
                    .sorted(Comparator.comparing(UserCard::dueAt))
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<UserCard> findByProfileId(long profileId, int offset, int limit) {
            return all.stream()
                    .filter(card -> card.profileId() == profileId)
                    .sorted(Comparator.comparing(UserCard::createdAt).thenComparing(UserCard::id).reversed())
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }

        @Override
        public long countByProfileId(long profileId) {
            return all.stream().filter(card -> card.profileId() == profileId).count();
        }

        @Override
        public long countCreatedSince(Collection<Long> profileIds, Instant since) {
            return all.stream()
                    .filter(card -> profileIds.contains(card.profileId()) && !card.createdAt().isBefore(since))
                    .count();
        }
    }

    public static class Profiles implements LearningProfileRepository {

        public final Map<Long, LearningProfile> byId = new LinkedHashMap<>();
        private long nextId = 100;

        @Override
        public LearningProfile save(LearningProfile profile) {
            long id = profile.id() != null ? profile.id() : nextId++;
            var saved = new LearningProfile(id, profile.userId(), profile.targetLanguage(), profile.nativeLanguage(),
                    profile.level(), profile.reminderTime(), profile.createdAt());
            byId.put(id, saved);
            return saved;
        }

        @Override
        public Optional<LearningProfile> findById(long id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public List<LearningProfile> findByUserId(long userId) {
            return new ArrayList<>(byId.values().stream().filter(p -> p.userId() == userId).toList());
        }

        @Override
        public Optional<LearningProfile> findByUserIdAndTargetLanguage(long userId, Language targetLanguage) {
            return findByUserId(userId).stream().filter(p -> p.targetLanguage() == targetLanguage).findFirst();
        }
    }
}
