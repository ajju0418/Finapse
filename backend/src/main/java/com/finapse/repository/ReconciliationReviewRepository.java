package com.finapse.repository;

import com.finapse.entity.ReconciliationReview;
import com.finapse.enums.ReviewStatus;
import com.finapse.enums.ReviewType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface ReconciliationReviewRepository extends JpaRepository<ReconciliationReview, UUID> {

    List<ReconciliationReview> findByStatusOrderByCreatedAtDesc(ReviewStatus status);

    List<ReconciliationReview> findByReviewTypeAndStatus(ReviewType reviewType, ReviewStatus status);

    long countByStatus(ReviewStatus status);

    @Modifying
    @Query("""
        DELETE FROM ReconciliationReview r 
        WHERE r.transactionLink.id IN (
            SELECT tl.id FROM TransactionLink tl 
            WHERE tl.sourceTransaction.id IN (SELECT t.id FROM Transaction t WHERE t.statement.id = :statementId) 
               OR tl.targetTransaction.id IN (SELECT t.id FROM Transaction t WHERE t.statement.id = :statementId)
        )
    """)
    void deleteByStatementId(@Param("statementId") UUID statementId);
}
