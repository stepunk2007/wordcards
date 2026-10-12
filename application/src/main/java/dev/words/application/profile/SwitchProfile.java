package dev.words.application.profile;

import dev.words.application.error.NotFoundException;
import dev.words.application.port.LearningProfileRepository;
import dev.words.application.port.UserRepository;
import dev.words.domain.LearningProfile;

import java.util.Objects;

/**
 * Makes one of the user's profiles active: new words go to its deck.
 */
public class SwitchProfile {

    private final UserRepository users;
    private final LearningProfileRepository profiles;

    public SwitchProfile(UserRepository users, LearningProfileRepository profiles) {
        this.users = users;
        this.profiles = profiles;
    }

    public LearningProfile switchTo(long userId, long profileId) {
        var user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " not found"));
        var profile = profiles.findById(profileId)
                .filter(found -> Objects.equals(found.userId(), user.id()))
                .orElseThrow(() -> new NotFoundException("Profile " + profileId + " not found"));

        if (!Objects.equals(user.activeProfileId(), profile.id())) {
            user.activateProfile(profile);
            users.save(user);
        }
        return profile;
    }
}
