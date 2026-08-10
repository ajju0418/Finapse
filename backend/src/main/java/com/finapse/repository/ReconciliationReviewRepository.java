package com.finapse.repository;

import com.finapse.entity.ReconciliationReview;
import com.finapse.enums.ReviewStatus;
import com.finapse.enums.ReviewType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReconciliationReviewRepository extends JpaRepository<ReconciliationReview, UUID> {

    List<ReconciliationReview> findByStatusOrderByCreatedAtDesc(ReviewStatus status);

    List<ReconciliationReview> findByReviewTypeAndStatus(ReviewType reviewType, ReviewStatus status);

    long countByStatus(ReviewStatus status);
}
