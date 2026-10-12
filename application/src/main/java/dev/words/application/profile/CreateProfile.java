package dev.words.application.profile;

import dev.words.application.error.NotFoundException;
import dev.words.application.port.LearningProfileRepository;
import dev.words.application.port.UserRepository;
import dev.words.domain.CefrLevel;
import dev.words.domain.Language;
import dev.words.domain.LearningProfile;

import java.time.Clock;

/**
 * Adds a language the user wants to learn. The user's first profile becomes active right away;
 * later ones are activated explicitly with {@link SwitchProfile}.
 */
public class CreateProfile {

    private final UserRepository users;
    private final LearningProfileRepository profiles;
    private final Clock clock;

    public CreateProfile(UserRepository users, LearningProfileRepository profiles, Clock clock) {
        this.users = users;
        this.profiles = profiles;
        this.clock = clock;
    }

    public LearningProfile create(long userId, Language targetLanguage, Language nativeLanguage, CefrLevel level) {
        var user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " not found"));
        if (profiles.findByUserIdAndTargetLanguage(userId, targetLanguage).isPresent()) {
            throw new ProfileAlreadyExistsException(targetLanguage);
        }

        var profile = profiles.save(
                LearningProfile.create(userId, targetLanguage, nativeLanguage, level, clock.instant()));

        if (!user.hasActiveProfile()) {
            user.activateProfile(profile);
            users.save(user);
        }
        return profile;
    }
}
