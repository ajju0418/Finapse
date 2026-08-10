package com.finapse.repository;

import com.finapse.entity.TransactionLink;
import com.finapse.enums.TransactionLinkStatus;
import com.finapse.enums.TransactionLinkType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionLinkRepository extends JpaRepository<TransactionLink, UUID> {

    List<TransactionLink> findBySourceTransactionId(UUID sourceTransactionId);

    List<TransactionLink> findByTargetTransactionId(UUID targetTransactionId);

    List<TransactionLink> findByStatus(TransactionLinkStatus status);

    List<TransactionLink> findByLinkTypeAndStatus(TransactionLinkType linkType, TransactionLinkStatus status);

    boolean existsBySourceTransactionIdAndTargetTransactionIdAndLinkType(
            UUID sourceTransactionId, UUID targetTransactionId, TransactionLinkType linkType);
}
