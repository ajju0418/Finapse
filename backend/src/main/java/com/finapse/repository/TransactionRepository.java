package com.finapse.repository;

import com.finapse.entity.Transaction;
import com.finapse.enums.ReconciliationStatus;
import com.finapse.enums.TransactionDirection;
import com.finapse.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByStatementIdOrderByTransactionDateDesc(UUID statementId);

    void deleteByStatementId(UUID statementId);

    List<Transaction> findByAccountIdOrderByTransactionDateDesc(UUID accountId);

    List<Transaction> findByCardIdOrderByTransactionDateDesc(UUID cardId);

    Optional<Transaction> findByTransactionHash(String transactionHash);

    List<Transaction> findByTransactionHashAndIdNot(String transactionHash, UUID excludeId);

    List<Transaction> findByReconciliationStatus(ReconciliationStatus status);

    boolean existsByStatementIdAndReconciliationStatus(UUID statementId, ReconciliationStatus status);

    List<Transaction> findByCardIdAndTransactionType(UUID cardId, TransactionType type);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.statement.user.id = :userId
              AND t.transactionDate BETWEEN :from AND :to
              AND t.transactionType = :type
            """)
    List<Transaction> findByUserAndDateRangeAndType(
            @Param("userId") UUID userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("type") TransactionType type);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.statement.user.id = :userId
              AND t.transactionDate BETWEEN :from AND :to
            """)
    List<Transaction> findByUserAndDateRange(
            @Param("userId") UUID userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
            WHERE t.statement.user.id = :userId
              AND t.transactionDate BETWEEN :from AND :to
              AND t.transactionType = :type
            """)
    BigDecimal sumAmountByUserAndDateRangeAndType(
            @Param("userId") UUID userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("type") TransactionType type);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.statement.user.id = :userId
              AND t.transactionDate BETWEEN :from AND :to
              AND t.transactionType IN :types
            ORDER BY t.transactionDate DESC
            """)
    List<Transaction> findByUserAndDateRangeAndTypes(
            @Param("userId") UUID userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("types") List<TransactionType> types);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.statement.user.id = :userId
            ORDER BY t.transactionDate DESC
            LIMIT :limit
            """)
    List<Transaction> findRecentByUser(
            @Param("userId") UUID userId,
            @Param("limit") int limit);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.statement.user.id = :userId
              AND t.transactionDate BETWEEN :from AND :to
              AND t.transactionType = :type
              AND t.direction = :direction
            """)
    List<Transaction> findByUserDateRangeTypeAndDirection(
            @Param("userId") UUID userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("type") TransactionType type,
            @Param("direction") TransactionDirection direction);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.statement.user.id = :userId
              AND t.description LIKE CONCAT('%', :pattern, '%')
              AND t.classificationSource IN ('USER_OVERRIDE', 'MERCHANT_DATABASE', 'EXACT_RULE')
              AND t.classificationConfidence >= 0.80
            ORDER BY t.transactionDate DESC
            LIMIT 20
            """)
    List<Transaction> findHighConfidenceByUserAndPattern(
            @Param("userId") UUID userId,
            @Param("pattern") String pattern);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.statement.user.id = :userId
              AND t.merchant.id = :merchantId
              AND t.classificationConfidence >= 0.80
            ORDER BY t.transactionDate DESC
            LIMIT 10
            """)
    List<Transaction> findByUserAndMerchant(
            @Param("userId") UUID userId,
            @Param("merchantId") UUID merchantId);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.statement.user.id = :userId
              AND t.merchant.id = :merchantId
              AND t.amount = :amount
            ORDER BY t.transactionDate DESC
            """)
    List<Transaction> findByUserMerchantAndAmount(
            @Param("userId") UUID userId,
            @Param("merchantId") UUID merchantId,
            @Param("amount") BigDecimal amount);

    /** Every transaction the recurring detector has grouped, for the subscriptions view. */
    @Query("""
            SELECT t FROM Transaction t
            LEFT JOIN FETCH t.merchant
            LEFT JOIN FETCH t.category
            WHERE t.statement.user.id = :userId
              AND t.isRecurring = true
              AND t.recurringGroupId IS NOT NULL
            ORDER BY t.transactionDate ASC
            """)
    List<Transaction> findRecurringByUser(@Param("userId") UUID userId);

    /** Ownership-scoped lookup; prevents reading another user's transaction by id. */
    @Query("""
            SELECT t FROM Transaction t
            WHERE t.id = :id
              AND t.statement.user.id = :userId
            """)
    Optional<Transaction> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
}
