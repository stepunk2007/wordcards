package dev.words.application.profile;

import dev.words.application.port.LearningProfileRepository;
import dev.words.application.port.UserRepository;
import dev.words.domain.Language;
import dev.words.domain.LearningProfile;
import dev.words.domain.User;

import java.util.ArrayList;
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
