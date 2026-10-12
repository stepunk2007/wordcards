package dev.words.infra.db;

import dev.words.application.port.ReviewLogRepository;
import dev.words.domain.ReviewLog;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional(readOnly = true)
class JpaReviewLogRepository implements ReviewLogRepository {

    private final ReviewLogJpaRepository jpa;

    JpaReviewLogRepository(ReviewLogJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public ReviewLog save(ReviewLog log) {
        return jpa.saveAndFlush(ReviewLogEntity.fromDomain(log)).toDomain();
    }

    @Override
    public List<ReviewLog> findByUserCardId(long userCardId) {
        return jpa.findByUserCardIdOrderByReviewedAtAscIdAsc(userCardId).stream()
                .map(ReviewLogEntity::toDomain).toList();
    }
}
