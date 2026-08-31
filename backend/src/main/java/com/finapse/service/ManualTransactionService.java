package com.finapse.service;

import com.finapse.dto.ManualTransactionRequest;
import com.finapse.dto.TransactionResponse;
import com.finapse.entity.Account;
import com.finapse.entity.Card;
import com.finapse.entity.Statement;
import com.finapse.entity.Transaction;
import com.finapse.entity.User;
import com.finapse.enums.ClassificationSource;
import com.finapse.enums.ImportStatus;
import com.finapse.enums.ReconciliationStatus;
import com.finapse.enums.StatementType;
import com.finapse.exception.BadRequestException;
import com.finapse.repository.StatementRepository;
import com.finapse.repository.TransactionRepository;
import com.finapse.util.HashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Cash and other off-statement spending. Every transaction still belongs to a
 * statement (the schema requires it), so manual entries are collected into a
 * single synthetic "Manual entries" statement per account or card.
 */
@Service
@RequiredArgsConstructor
public class ManualTransactionService {

    private static final String MANUAL_FILE_NAME = "Manual entries";

    private final TransactionRepository transactionRepository;
    private final StatementRepository statementRepository;
    private final CategoryService categoryService;
    private final MerchantService merchantService;
    private final AccountService accountService;
    private final CardService cardService;
    private final UserService userService;

    @Transactional
    public TransactionResponse create(ManualTransactionRequest request) {
        if ((request.accountId() == null) == (request.cardId() == null)) {
            throw new BadRequestException("Select exactly one source — either a bank account or a card.");
        }
        if (request.transactionDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("A transaction cannot be dated in the future.");
        }

        User user = userService.getCurrentUser();
        Account account = request.accountId() != null ? accountService.findOrThrow(request.accountId()) : null;
        Card card = request.cardId() != null ? cardService.findOrThrow(request.cardId()) : null;

        Statement statement = manualStatement(user, account, card);

        Transaction tx = new Transaction();
        tx.setStatement(statement);
        tx.setAccount(account);
        tx.setCard(card);
        tx.setTransactionDate(request.transactionDate());
        tx.setDescription(request.description().trim());
        tx.setAmount(request.amount());
        tx.setDirection(request.direction());
        tx.setTransactionType(request.transactionType());
        tx.setReconciliationStatus(ReconciliationStatus.UNMATCHED);
        tx.setClassificationSource(ClassificationSource.USER_OVERRIDE);
        tx.setClassificationConfidence(1.0);
        tx.setClassificationReason("Added manually by you");
        tx.setIsRecurring(false);
        tx.setTransactionHash(HashUtil.sha256(String.join("|",
                statement.getId().toString(),
                request.transactionDate().toString(),
                request.description().trim().toLowerCase(),
                request.amount().toPlainString(),
                request.direction().name(),
                UUID.randomUUID().toString())));

        if (request.categoryId() != null) {
            tx.setCategory(categoryService.findOrThrow(request.categoryId()));
        }
        tx.setMerchant(merchantService.resolveForTransaction(tx));

        Transaction saved = transactionRepository.save(tx);

        statement.setTransactionCount(statement.getTransactionCount() + 1);
        statement.setPeriodStart(min(statement.getPeriodStart(), saved.getTransactionDate()));
        statement.setPeriodEnd(max(statement.getPeriodEnd(), saved.getTransactionDate()));
        statementRepository.save(statement);

        return TransactionResponse.from(saved);
    }

    @Transactional
    public void delete(UUID transactionId) {
        Transaction tx = transactionRepository
                .findByIdAndUserId(transactionId, userService.getCurrentUserId())
                .orElseThrow(() -> new com.finapse.exception.ResourceNotFoundException(
                        "Transaction not found: " + transactionId));

        if (!isManual(tx.getStatement())) {
            throw new BadRequestException(
                    "Only manually added transactions can be deleted. Imported transactions are part of a statement — "
                    + "delete the statement instead.");
        }
        Statement statement = tx.getStatement();
        transactionRepository.delete(tx);
        statement.setTransactionCount(Math.max(0, statement.getTransactionCount() - 1));
        statementRepository.save(statement);
    }

    // -------------------------------------------------------------------------

    private boolean isManual(Statement statement) {
        return statement != null && MANUAL_FILE_NAME.equals(statement.getOriginalFileName());
    }

    /** Finds, or lazily creates, the manual-entry statement for this source. */
    private Statement manualStatement(User user, Account account, Card card) {
        String sourceId = account != null ? account.getId().toString() : card.getId().toString();
        String fileHash = HashUtil.sha256("manual:" + user.getId() + ":" + sourceId);

        return statementRepository.findByUserIdAndFileHash(user.getId(), fileHash)
                .orElseGet(() -> {
                    Statement statement = new Statement();
                    statement.setUser(user);
                    statement.setAccount(account);
                    statement.setCard(card);
                    statement.setStatementType(account != null ? StatementType.BANK : StatementType.CREDIT_CARD);
                    statement.setOriginalFileName(MANUAL_FILE_NAME);
                    statement.setFileHash(fileHash);
                    statement.setImportStatus(ImportStatus.COMPLETED);
                    return statementRepository.save(statement);
                });
    }

    private LocalDate min(LocalDate current, LocalDate candidate) {
        return current == null || candidate.isBefore(current) ? candidate : current;
    }

    private LocalDate max(LocalDate current, LocalDate candidate) {
        return current == null || candidate.isAfter(current) ? candidate : current;
    }
}
