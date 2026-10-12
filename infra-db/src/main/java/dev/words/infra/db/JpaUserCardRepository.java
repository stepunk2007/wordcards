package dev.words.infra.db;

import dev.words.application.port.UserCardRepository;
import dev.words.domain.UserCard;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
class JpaUserCardRepository implements UserCardRepository {

    private final UserCardJpaRepository jpa;

    JpaUserCardRepository(UserCardJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public UserCard save(UserCard card) {
        return jpa.saveAndFlush(UserCardEntity.fromDomain(card)).toDomain();
    }

    @Override
    public Optional<UserCard> findById(long id) {
        return jpa.findById(id).map(UserCardEntity::toDomain);
    }

    @Override
    public Optional<UserCard> findByProfileIdAndLexemeId(long profileId, long lexemeId) {
        return jpa.findByProfileIdAndLexemeId(profileId, lexemeId).map(UserCardEntity::toDomain);
    }

    @Override
    public List<UserCard> findDue(long profileId, Instant now, int limit) {
        return jpa.findByProfileIdAndDueAtLessThanEqualOrderByDueAtAscIdAsc(profileId, now, Limit.of(limit))
                .stream().map(UserCardEntity::toDomain).toList();
    }

    @Override
    public long countByProfileId(long profileId) {
        return jpa.countByProfileId(profileId);
    }
}
