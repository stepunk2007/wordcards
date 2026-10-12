package dev.words.application.port;

import dev.words.domain.UserCard;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserCardRepository {

    UserCard save(UserCard card);

    Optional<UserCard> findById(long id);

    Optional<UserCard> findByProfileIdAndLexemeId(long profileId, long lexemeId);

    /**
     * @return cards of the profile due at {@code now}, most overdue first
     */
    List<UserCard> findDue(long profileId, Instant now, int limit);

    /**
     * @return a page of the profile's deck, newest cards first
     */
    List<UserCard> findByProfileId(long profileId, int offset, int limit);

    long countByProfileId(long profileId);

    /**
     * @return number of cards of the profile due at {@code now}
     */
    long countDue(long profileId, Instant now);

    /**
     * @return number of cards added to any of the profiles at or after {@code since}
     */
    long countCreatedSince(Collection<Long> profileIds, Instant since);
}
