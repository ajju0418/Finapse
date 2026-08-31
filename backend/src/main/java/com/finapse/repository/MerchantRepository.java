package com.finapse.repository;

import com.finapse.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

    Optional<Merchant> findByNormalizedName(String normalizedName);

    @Query("SELECT m FROM Merchant m WHERE :narration LIKE CONCAT('%', m.normalizedName, '%') ORDER BY LENGTH(m.normalizedName) DESC")
    List<Merchant> findByNarrationContaining(@Param("narration") String narration);

    @Query("SELECT m FROM Merchant m WHERE m.normalizedName LIKE CONCAT('%', :token, '%')")
    List<Merchant> findByNormalizedNameContaining(@Param("token") String token);
}
