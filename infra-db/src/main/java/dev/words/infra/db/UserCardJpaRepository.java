package dev.words.infra.db;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

interface UserCardJpaRepository extends JpaRepository<UserCardEntity, Long> {

    Optional<UserCardEntity> findByProfileIdAndLexemeId(Long profileId, Long lexemeId);

    List<UserCardEntity> findByProfileIdAndDueAtLessThanEqualOrderByDueAtAscIdAsc(
            Long profileId, Instant now, Limit limit);

    List<UserCardEntity> findByProfileIdOrderByCreatedAtDescIdDesc(Long profileId, Pageable pageable);

    long countByProfileId(Long profileId);

    long countByProfileIdAndDueAtLessThanEqual(Long profileId, Instant now);

    long countByProfileIdInAndCreatedAtGreaterThanEqual(Collection<Long> profileIds, Instant since);
}
