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

    List<Transaction> findByAccountIdOrderByTransactionDateDesc(UUID accountId);

    List<Transaction> findByCardIdOrderByTransactionDateDesc(UUID cardId);

    Optional<Transaction> findByTransactionHash(String transactionHash);

    List<Transaction> findByTransactionHashAndIdNot(String transactionHash, UUID excludeId);

    List<Transaction> findByReconciliationStatus(ReconciliationStatus status);

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
}
