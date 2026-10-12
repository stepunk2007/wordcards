package dev.words.application.port;

import dev.words.domain.UserCard;

import java.time.Instant;
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

    long countByProfileId(long profileId);
}
