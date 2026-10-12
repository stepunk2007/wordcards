package dev.words.application.port;

import dev.words.domain.ReviewLog;

import java.util.List;

public interface ReviewLogRepository {

    ReviewLog save(ReviewLog log);

    /**
     * @return reviews of the card, oldest first
     */
    List<ReviewLog> findByUserCardId(long userCardId);
}
