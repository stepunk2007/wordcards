package dev.words.application.review;

import dev.words.application.error.NoActiveProfileException;
import dev.words.application.error.NotFoundException;
import dev.words.application.port.LearningProfileRepository;
import dev.words.application.port.UserRepository;
import dev.words.domain.LearningProfile;

/**
 * Finds the profile the user is working with right now.
 */
final class ActiveProfiles {

    private ActiveProfiles() {
    }

    static LearningProfile of(long userId, UserRepository users, LearningProfileRepository profiles) {
        var user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " not found"));
        if (!user.hasActiveProfile()) {
            throw new NoActiveProfileException(userId);
        }
        return profiles.findById(user.activeProfileId())
                .orElseThrow(() -> new NoActiveProfileException(userId));
    }
}
