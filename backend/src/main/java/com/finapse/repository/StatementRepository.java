package com.finapse.repository;

import com.finapse.entity.Statement;
import com.finapse.enums.ImportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StatementRepository extends JpaRepository<Statement, UUID> {

    List<Statement> findByUserIdOrderByUploadedAtDesc(UUID userId);

    Optional<Statement> findByUserIdAndFileHash(UUID userId, String fileHash);

    List<Statement> findByUserIdAndImportStatus(UUID userId, ImportStatus importStatus);
}
