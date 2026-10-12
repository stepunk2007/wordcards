package dev.words.infra.db;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ReviewLogJpaRepository extends JpaRepository<ReviewLogEntity, Long> {

    List<ReviewLogEntity> findByUserCardIdOrderByReviewedAtAscIdAsc(Long userCardId);
}
