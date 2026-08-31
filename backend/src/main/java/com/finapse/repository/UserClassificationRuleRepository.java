package com.finapse.repository;

import com.finapse.entity.UserClassificationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserClassificationRuleRepository extends JpaRepository<UserClassificationRule, UUID> {

    List<UserClassificationRule> findByUserIdAndIsActiveTrueOrderByTimesAppliedDesc(UUID userId);

    @Query("""
            SELECT r FROM UserClassificationRule r
            WHERE r.user.id = :userId
              AND r.isActive = true
              AND r.narrationPattern = :pattern
            """)
    List<UserClassificationRule> findExactRulesByUserAndPattern(
            @Param("userId") UUID userId,
            @Param("pattern") String pattern);

    @Modifying
    @Query("UPDATE UserClassificationRule r SET r.timesApplied = r.timesApplied + 1 WHERE r.id = :id")
    void incrementTimesApplied(@Param("id") UUID id);
}
