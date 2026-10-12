package dev.words.application.profile;

import dev.words.application.error.NotFoundException;
import dev.words.application.port.LearningProfileRepository;
import dev.words.application.port.UserCardRepository;
import dev.words.application.port.UserRepository;

import java.util.List;
import java.util.Objects;

/**
 * Lists the user's profiles, oldest first, with the size of each deck.
 */
public class ListProfiles {

    private final UserRepository users;
    private final LearningProfileRepository profiles;
    private final UserCardRepository cards;

    public ListProfiles(UserRepository users, LearningProfileRepository profiles, UserCardRepository cards) {
        this.users = users;
        this.profiles = profiles;
        this.cards = cards;
    }

    public List<ProfileSummary> list(long userId) {
        var user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " not found"));
        // One count per profile: a user learns only a handful of languages.
        return profiles.findByUserId(userId).stream()
                .map(profile -> new ProfileSummary(
                        profile,
                        cards.countByProfileId(profile.id()),
                        Objects.equals(profile.id(), user.activeProfileId())))
                .toList();
    }
}
