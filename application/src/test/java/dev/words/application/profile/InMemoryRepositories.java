package dev.words.application.profile;

import dev.words.application.port.LearningProfileRepository;
import dev.words.application.port.UserCardRepository;
import dev.words.application.port.UserRepository;
import dev.words.domain.Language;
import dev.words.domain.LearningProfile;
import dev.words.domain.User;
import dev.words.domain.UserCard;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class InMemoryRepositories {

    static class Users implements UserRepository {

        final Map<Long, User> byId = new LinkedHashMap<>();
        int saves;
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

    static class Cards implements UserCardRepository {

        final List<UserCard> all = new ArrayList<>();
        private long nextId = 1000;

        @Override
        public UserCard save(UserCard card) {
            var saved = new UserCard(card.id() != null ? card.id() : nextId++, card.profileId(), card.lexemeId(),
                    card.state(), card.dueAt(), card.stability(), card.difficulty(), card.reps(), card.lapses(),
                    card.createdAt());
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
        public long countByProfileId(long profileId) {
            return all.stream().filter(card -> card.profileId() == profileId).count();
        }
    }

    static class Profiles implements LearningProfileRepository {

        final Map<Long, LearningProfile> byId = new LinkedHashMap<>();
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
