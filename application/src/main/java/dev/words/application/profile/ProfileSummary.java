package dev.words.application.profile;

import dev.words.domain.LearningProfile;

/**
 * A learning profile as shown in the language switcher.
 *
 * @param wordCount number of words in the profile's deck
 * @param active    whether new words currently go to this profile
 */
public record ProfileSummary(LearningProfile profile, long wordCount, boolean active) {
}
