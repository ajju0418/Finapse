package com.finapse.service;

import com.finapse.classification.orchestrator.ClassificationOrchestrator;
import com.finapse.dto.ColumnMappingOverride;
import com.finapse.dto.StatementParseResult;
import com.finapse.entity.Statement;
import com.finapse.entity.Transaction;
import com.finapse.enums.ImportStatus;
import com.finapse.repository.StatementRepository;
import com.finapse.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Runs the parse → classify → reconcile pipeline off the request thread.
 *
 * <p>Kept in its own bean so Spring's {@code @Async} proxy applies; a self-call
 * from {@link StatementService} would bypass it and run inline.
 */
@Slf4j
@Service
public class StatementImportProcessor {

    private final StatementRepository statementRepository;
    private final TransactionRepository transactionRepository;
    private final StatementParserFactory parserFactory;
    private final ClassificationOrchestrator classificationOrchestrator;
    private final DuplicateDetectionService duplicateDetectionService;
    private final ReconciliationService reconciliationService;

    /**
     * Spring-managed reference to this bean so {@code @Transactional} on {@link #process}
     * and {@link #markFailed} is honoured. A direct {@code this.process(...)} call from
     * {@link #processAsync} would bypass the proxy and run without a session, causing
     * lazy-loaded relations (e.g. Statement.account.user) to fail with LazyInitializationException.
     */
    private final StatementImportProcessor self;

    public StatementImportProcessor(
            StatementRepository statementRepository,
            TransactionRepository transactionRepository,
            StatementParserFactory parserFactory,
            ClassificationOrchestrator classificationOrchestrator,
            DuplicateDetectionService duplicateDetectionService,
            ReconciliationService reconciliationService,
            @Lazy StatementImportProcessor self) {
        this.statementRepository = statementRepository;
        this.transactionRepository = transactionRepository;
        this.parserFactory = parserFactory;
        this.classificationOrchestrator = classificationOrchestrator;
        this.duplicateDetectionService = duplicateDetectionService;
        this.reconciliationService = reconciliationService;
        this.self = self;
    }

    @Async("statementImportExecutor")
    public void processAsync(UUID statementId, byte[] fileBytes, String fileName,
                             ColumnMappingOverride override, UUID userId) {
        try {
            self.process(statementId, fileBytes, fileName, override, userId);
        } catch (Exception e) {
            log.error("Import failed for statement {}", statementId, e);
            self.markFailed(statementId, e.getMessage());
        }
    }

    /**
     * Parses and persists the statement's transactions.
     *
     * <p>The security context is not propagated to the async thread, so the owning
     * user is passed explicitly and every lookup is derived from the statement.
     */
    @Transactional
    public void process(UUID statementId, byte[] fileBytes, String fileName,
                        ColumnMappingOverride override, UUID userId) {
        Statement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new IllegalStateException("Statement disappeared mid-import: " + statementId));

        StatementFileParser parser = parserFactory.getParser(fileName);
        StatementParseResult parseResult = parser.parse(
                new ByteArrayInputStream(fileBytes), fileName,
                override != null ? override : ColumnMappingOverride.NONE);

        if (parseResult.records().isEmpty()) {
            statement.setImportStatus(ImportStatus.FAILED);
            statement.setImportError("No valid transactions could be extracted from the uploaded file.");
            statement.setProcessedAt(LocalDateTime.now());
            statementRepository.save(statement);
            return;
        }

        List<Transaction> transactions = new ArrayList<>();
        for (var raw : parseResult.records()) {
            transactions.add(classificationOrchestrator.orchestrate(
                    raw, statement, statement.getAccount(), statement.getCard()));
        }
        transactionRepository.saveAll(transactions);

        duplicateDetectionService.detectDuplicates(transactions);
        reconciliationService.reconcile(transactions, userId);

        LocalDate periodStart = transactions.stream()
                .map(Transaction::getTransactionDate).min(LocalDate::compareTo).orElse(null);
        LocalDate periodEnd = transactions.stream()
                .map(Transaction::getTransactionDate).max(LocalDate::compareTo).orElse(null);

        boolean hasInvalidRows = !parseResult.invalidRows().isEmpty();
        statement.setTransactionCount(transactions.size());
        statement.setPeriodStart(periodStart);
        statement.setPeriodEnd(periodEnd);
        statement.setImportStatus(hasInvalidRows ? ImportStatus.REVIEW_REQUIRED : ImportStatus.COMPLETED);
        statement.setImportError(hasInvalidRows
                ? parseResult.invalidRows().size() + " row(s) could not be read and were skipped."
                : null);
        statement.setProcessedAt(LocalDateTime.now());
        statementRepository.save(statement);

        log.info("Statement {} processed: {} transactions, {} invalid rows",
                statementId, transactions.size(), parseResult.invalidRows().size());
    }

    /** Runs in its own transaction so the failure is recorded even after a rollback. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID statementId, String reason) {
        statementRepository.findById(statementId).ifPresent(statement -> {
            statement.setImportStatus(ImportStatus.FAILED);
            statement.setImportError(truncate(reason));
            statement.setProcessedAt(LocalDateTime.now());
            statementRepository.save(statement);
        });
    }

    private String truncate(String reason) {
        if (reason == null || reason.isBlank()) return "The import failed for an unknown reason.";
        return reason.length() <= 500 ? reason : reason.substring(0, 497) + "...";
    }
}
