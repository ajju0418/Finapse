package com.finapse.service;

import com.finapse.classification.orchestrator.ClassificationOrchestrator;
import com.finapse.dto.ColumnMappingOverride;
import com.finapse.dto.StatementParseResult;
import com.finapse.dto.StatementPreviewResponse;
import com.finapse.dto.StatementResponse;
import com.finapse.entity.Account;
import com.finapse.entity.Card;
import com.finapse.entity.Statement;
import com.finapse.entity.Transaction;
import com.finapse.enums.ImportStatus;
import com.finapse.enums.StatementType;
import com.finapse.exception.DuplicateStatementException;
import com.finapse.exception.InvalidStatementFileException;
import com.finapse.exception.StatementProcessingException;
import com.finapse.repository.ReconciliationReviewRepository;
import com.finapse.repository.StatementRepository;
import com.finapse.repository.TransactionLinkRepository;
import com.finapse.repository.TransactionRepository;
import com.finapse.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatementService {

    private final StatementRepository statementRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionLinkRepository transactionLinkRepository;
    private final ReconciliationReviewRepository reconciliationReviewRepository;
    private final UserService userService;
    private final AccountService accountService;
    private final CardService cardService;
    private final StatementParserFactory parserFactory;
    private final ClassificationOrchestrator classificationOrchestrator;
    private final StatementImportProcessor importProcessor;

    @Transactional(readOnly = true)
    public List<StatementResponse> getAll() {
        UUID userId = userService.getCurrentUserId();
        return statementRepository.findByUserIdOrderByUploadedAtDesc(userId)
                .stream()
                .map(StatementResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StatementResponse getById(UUID id) {
        return StatementResponse.from(findOrThrow(id));
    }

    @Transactional
    public void delete(UUID id) {
        Statement statement = findOrThrow(id);
        reconciliationReviewRepository.deleteByStatementId(id);
        transactionLinkRepository.deleteByStatementId(id);
        transactionRepository.deleteByStatementId(id);
        statementRepository.delete(statement);
    }

    /**
     * Dry run. Parses the file in memory and reports the columns Finapse detected
     * plus a handful of sample rows, so the user can remap columns before
     * committing. Nothing is persisted.
     */
    public StatementPreviewResponse preview(MultipartFile file, ColumnMappingOverride override) {
        return preview(file, override, null, null, null);
    }

    public StatementPreviewResponse preview(MultipartFile file, ColumnMappingOverride override,
                                           String password, UUID accountId, UUID cardId) {
        validateFileType(file);

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new StatementProcessingException("Could not read uploaded file.");
        }

        String fileName = file.getOriginalFilename();
        StatementFileParser parser = parserFactory.getParser(fileName);
        ColumnMappingOverride effective = override != null ? override : ColumnMappingOverride.NONE;

        String effectivePassword = password;
        if ((effectivePassword == null || effectivePassword.isBlank()) && accountId != null) {
            Account account = accountService.findOrThrow(accountId);
            effectivePassword = account.getStatementPassword();
        } else if ((effectivePassword == null || effectivePassword.isBlank()) && cardId != null) {
            Card card = cardService.findOrThrow(cardId);
            effectivePassword = card.getStatementPassword();
        }

        List<String> columns;
        ColumnMappingOverride detected;
        try {
            columns = parser.readColumnNames(new java.io.ByteArrayInputStream(fileBytes), fileName, effectivePassword);
            detected = parser.detectMapping(new java.io.ByteArrayInputStream(fileBytes), fileName, effectivePassword);
        } catch (com.finapse.exception.EncryptedPdfException e) {
            return new StatementPreviewResponse(fileName, List.of(), ColumnMappingOverride.NONE,
                    false, e.getMessage(), 0, 0, List.of(), List.of(), true);
        } catch (InvalidStatementFileException e) {
            return new StatementPreviewResponse(fileName, List.of(), ColumnMappingOverride.NONE,
                    false, e.getMessage(), 0, 0, List.of(), List.of(), false);
        }

        StatementParseResult result;
        try {
            result = parser.parse(new java.io.ByteArrayInputStream(fileBytes), fileName, effective, effectivePassword);
        } catch (com.finapse.exception.EncryptedPdfException e) {
            return new StatementPreviewResponse(fileName, columns, detected,
                    false, e.getMessage(), 0, 0, List.of(), List.of(), true);
        } catch (InvalidStatementFileException e) {
            return new StatementPreviewResponse(fileName, columns, detected,
                    false, e.getMessage(), 0, 0, List.of(), List.of(), false);
        }

        List<StatementPreviewResponse.PreviewRow> sample = result.records().stream()
                .limit(10)
                .map(r -> new StatementPreviewResponse.PreviewRow(
                        r.sourceRowNumber(),
                        r.transactionDate() != null ? r.transactionDate().toString() : null,
                        r.description(),
                        r.amount() != null ? r.amount().toPlainString() : null,
                        r.direction() != null ? r.direction().name() : null))
                .toList();

        ColumnMappingOverride active = effective.isEmpty() ? detected : effective;

        return new StatementPreviewResponse(
                fileName, columns, active, true,
                result.records().size() + " transaction(s) ready to import.",
                result.records().size(),
                result.invalidRows().size(),
                sample,
                result.invalidRows().stream().limit(5).toList(),
                false);
    }

    /**
     * Accepts the upload, records the statement as PROCESSING and hands the heavy
     * parse/classify/reconcile work to a background worker. Clients poll
     * {@code GET /api/statements/{id}} until the status leaves PROCESSING.
     */
    @Transactional
    public StatementResponse upload(MultipartFile file,
                                    StatementType statementType,
                                    UUID accountId,
                                    UUID cardId,
                                    ColumnMappingOverride override) {
        return upload(file, statementType, accountId, cardId, override, null, false);
    }

    @Transactional
    public StatementResponse upload(MultipartFile file,
                                    StatementType statementType,
                                    UUID accountId,
                                    UUID cardId,
                                    ColumnMappingOverride override,
                                    String password,
                                    Boolean savePassword) {
        validateSource(statementType, accountId, cardId);
        validateFileType(file);

        Account account = accountId != null ? accountService.findOrThrow(accountId) : null;
        Card card = cardId != null ? cardService.findOrThrow(cardId) : null;

        String effectivePassword = password;
        if ((effectivePassword == null || effectivePassword.isBlank()) && account != null) {
            effectivePassword = account.getStatementPassword();
        } else if ((effectivePassword == null || effectivePassword.isBlank()) && card != null) {
            effectivePassword = card.getStatementPassword();
        }

        if (Boolean.TRUE.equals(savePassword) && password != null && !password.isBlank()) {
            if (account != null) {
                account.setStatementPassword(password);
            } else if (card != null) {
                card.setStatementPassword(password);
            }
        }

        // Compute file hash for duplicate detection
        String fileHash;
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
            fileHash = HashUtil.sha256(new java.io.ByteArrayInputStream(fileBytes));
        } catch (IOException e) {
            throw new StatementProcessingException("Could not read uploaded file.");
        }

        UUID userId = userService.getCurrentUserId();
        statementRepository.findByUserIdAndFileHash(userId, fileHash).ifPresent(existing -> {
            throw new DuplicateStatementException(
                    "This file has already been imported (statement ID: " + existing.getId() + "). " +
                    "Upload a different file or check your existing statements.");
        });

        // Fail fast on an unsupported format before creating a PROCESSING row the
        // background worker could never finish.
        parserFactory.getParser(file.getOriginalFilename());

        Statement statement = new Statement();
        statement.setUser(userService.getCurrentUser());
        statement.setStatementType(statementType);
        statement.setOriginalFileName(file.getOriginalFilename());
        statement.setFileHash(fileHash);
        statement.setImportStatus(ImportStatus.PROCESSING);
        statement.setAccount(account);
        statement.setCard(card);
        statement = statementRepository.save(statement);

        UUID statementId = statement.getId();
        String fileName = file.getOriginalFilename();
        String finalPassword = effectivePassword;

        // Dispatch only once this transaction has committed, otherwise the worker
        // can start before the statement row is visible to it.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                importProcessor.processAsync(statementId, fileBytes, fileName, override, userId, finalPassword);
            }
        });

        return StatementResponse.from(statement);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void validateSource(StatementType type, UUID accountId, UUID cardId) {
        if (type == StatementType.BANK) {
            if (accountId == null)
                throw new InvalidStatementFileException("A bank account must be selected for a BANK statement.");
            if (cardId != null)
                throw new InvalidStatementFileException("A BANK statement cannot be associated with a card.");
        } else {
            if (cardId == null)
                throw new InvalidStatementFileException("A credit card must be selected for a CREDIT_CARD statement.");
            if (accountId != null)
                throw new InvalidStatementFileException("A CREDIT_CARD statement cannot be associated with a bank account.");
        }
    }

    private void validateFileType(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || (!name.toLowerCase().endsWith(".csv") && !name.toLowerCase().endsWith(".xls") && !name.toLowerCase().endsWith(".xlsx") && !name.toLowerCase().endsWith(".pdf"))) {
            throw new InvalidStatementFileException("Only CSV, Excel, and PDF files are accepted. Please upload a .csv, .xls, .xlsx, or .pdf file.");
        }
        if (file.isEmpty()) {
            throw new InvalidStatementFileException("The uploaded file is empty.");
        }
    }

    /**
     * Re-runs classification and category inference on every transaction belonging to
     * this statement. Safe to call multiple times — idempotent.
     */
    @Transactional
    public StatementResponse reclassify(UUID statementId) {
        Statement statement = findOrThrow(statementId);
        List<Transaction> transactions = transactionRepository.findByStatementIdOrderByTransactionDateDesc(statementId);

        for (Transaction tx : transactions) {
            classificationOrchestrator.reclassify(tx);
        }
        transactionRepository.saveAll(transactions);

        log.info("Reclassified {} transactions for statement {}", transactions.size(), statementId);
        return StatementResponse.from(statement);
    }

    public Statement findOrThrow(UUID id) {
        return statementRepository.findByIdAndUserId(id, userService.getCurrentUserId())
                .orElseThrow(() -> new com.finapse.exception.ResourceNotFoundException(
                        "Statement not found: " + id));
    }
}
