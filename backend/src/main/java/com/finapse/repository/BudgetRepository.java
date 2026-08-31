package com.finapse.repository;

import com.finapse.entity.Budget;
import com.finapse.enums.BudgetPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    @Query("""
            SELECT b FROM Budget b
            LEFT JOIN FETCH b.category
            WHERE b.user.id = :userId
              AND b.active = true
            ORDER BY b.createdAt ASC
            """)
    List<Budget> findActiveByUser(@Param("userId") UUID userId);

    Optional<Budget> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
            SELECT b FROM Budget b
            WHERE b.user.id = :userId
              AND b.period = :period
              AND b.active = true
              AND ((:categoryId IS NULL AND b.category IS NULL)
                   OR b.category.id = :categoryId)
            """)
    Optional<Budget> findActiveByUserCategoryAndPeriod(@Param("userId") UUID userId,
                                                       @Param("categoryId") UUID categoryId,
                                                       @Param("period") BudgetPeriod period);
}
