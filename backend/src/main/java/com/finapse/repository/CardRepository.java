package com.finapse.repository;

import com.finapse.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID> {

    List<Card> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Card> findByUserIdAndIsActiveTrue(UUID userId);
}
