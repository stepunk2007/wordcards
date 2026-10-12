package dev.words.application.port;

import dev.words.domain.Language;
import dev.words.domain.LearningProfile;

import java.util.List;
import java.util.Optional;

public interface LearningProfileRepository {

    LearningProfile save(LearningProfile profile);

    Optional<LearningProfile> findById(long id);

    /**
     * @return profiles of the user, oldest first
     */
    List<LearningProfile> findByUserId(long userId);

    Optional<LearningProfile> findByUserIdAndTargetLanguage(long userId, Language targetLanguage);
}
