package com.finapse.service;

import com.finapse.classification.orchestrator.ClassificationOrchestrator;
import com.finapse.dto.StatementParseResult;
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
import com.finapse.repository.StatementRepository;
import com.finapse.repository.TransactionRepository;
import com.finapse.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatementService {

    private final StatementRepository statementRepository;
    private final TransactionRepository transactionRepository;
    private final UserService userService;
    private final AccountService accountService;
    private final CardService cardService;
    private final StatementParserFactory parserFactory;
    private final ClassificationOrchestrator classificationOrchestrator;
    private final DuplicateDetectionService duplicateDetectionService;
    private final ReconciliationService reconciliationService;

    @Transactional(readOnly = true)
    public List<StatementResponse> getAll() {
        UUID userId = userService.getDefaultUser().getId();
        return statementRepository.findByUserIdOrderByUploadedAtDesc(userId)
                .stream()
                .map(StatementResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StatementResponse getById(UUID id) {
        return StatementResponse.from(findOrThrow(id));
    }

    /**
     * Full pipeline:
     * 1. Validate file type
     * 2. Compute file hash — reject duplicate uploads
     * 3. Parse CSV
     * 4. Normalise transactions
     * 5. Persist statement + transactions
     * 6. Mark statement COMPLETED (or REVIEW_REQUIRED if invalid rows exist)
     */
    @Transactional
    public StatementResponse upload(MultipartFile file,
                                    StatementType statementType,
                                    UUID accountId,
                                    UUID cardId) {
        validateSource(statementType, accountId, cardId);
        validateFileType(file);

        Account account = accountId != null ? accountService.findOrThrow(accountId) : null;
        Card card = cardId != null ? cardService.findOrThrow(cardId) : null;

        // Compute file hash for duplicate detection
        String fileHash;
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
            fileHash = HashUtil.sha256(new java.io.ByteArrayInputStream(fileBytes));
        } catch (IOException e) {
            throw new StatementProcessingException("Could not read uploaded file.");
        }

        UUID userId = userService.getDefaultUser().getId();
        statementRepository.findByUserIdAndFileHash(userId, fileHash).ifPresent(existing -> {
            throw new DuplicateStatementException(
                    "This file has already been imported (statement ID: " + existing.getId() + "). " +
                    "Upload a different file or check your existing statements.");
        });

        // Create statement record
        Statement statement = new Statement();
        statement.setUser(userService.getDefaultUser());
        statement.setStatementType(statementType);
        statement.setOriginalFileName(file.getOriginalFilename());
        statement.setFileHash(fileHash);
        statement.setImportStatus(ImportStatus.PROCESSING);
        statement.setAccount(account);
        statement.setCard(card);
        statement = statementRepository.save(statement);

        // Parse File
        StatementParseResult parseResult;
        try {
            StatementFileParser parser = parserFactory.getParser(file.getOriginalFilename());
            parseResult = parser.parse(
                    new java.io.ByteArrayInputStream(fileBytes),
                    file.getOriginalFilename());
        } catch (InvalidStatementFileException e) {
            statement.setImportStatus(ImportStatus.FAILED);
            statementRepository.save(statement);
            throw e;
        }

        if (parseResult.records().isEmpty()) {
            statement.setImportStatus(ImportStatus.FAILED);
            statementRepository.save(statement);
            throw new InvalidStatementFileException(
                    "No valid transactions could be extracted from the uploaded file.");
        }

        // Normalise, classify, resolve merchant, persist
        List<Transaction> transactions = new ArrayList<>();
        for (var raw : parseResult.records()) {
            Transaction tx = classificationOrchestrator.orchestrate(raw, statement, account, card);
            transactions.add(tx);
        }
        transactionRepository.saveAll(transactions);

        // Duplicate detection + reconciliation (post-persist so IDs exist)
        duplicateDetectionService.detectDuplicates(transactions);
        reconciliationService.reconcile(transactions, userService.getDefaultUser().getId());

        // Compute period range
        LocalDate periodStart = transactions.stream()
                .map(Transaction::getTransactionDate)
                .min(LocalDate::compareTo).orElse(null);
        LocalDate periodEnd = transactions.stream()
                .map(Transaction::getTransactionDate)
                .max(LocalDate::compareTo).orElse(null);

        // Finalise statement
        boolean hasInvalidRows = !parseResult.invalidRows().isEmpty();
        statement.setTransactionCount(transactions.size());
        statement.setPeriodStart(periodStart);
        statement.setPeriodEnd(periodEnd);
        statement.setImportStatus(hasInvalidRows ? ImportStatus.REVIEW_REQUIRED : ImportStatus.COMPLETED);
        statement.setProcessedAt(LocalDateTime.now());
        statement = statementRepository.save(statement);

        log.info("Statement {} processed: {} transactions, {} invalid rows",
                statement.getId(), transactions.size(), parseResult.invalidRows().size());

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
        if (name == null || (!name.toLowerCase().endsWith(".csv") && !name.toLowerCase().endsWith(".xls") && !name.toLowerCase().endsWith(".xlsx"))) {
            throw new InvalidStatementFileException("Only CSV and Excel files are accepted. Please upload a .csv, .xls, or .xlsx file.");
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
        return statementRepository.findById(id)
                .orElseThrow(() -> new com.finapse.exception.ResourceNotFoundException(
                        "Statement not found: " + id));
    }
}
