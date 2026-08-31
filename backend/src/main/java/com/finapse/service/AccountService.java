package com.finapse.service;

import com.finapse.dto.AccountCreateRequest;
import com.finapse.dto.AccountResponse;
import com.finapse.dto.AccountAnalyticsResponse;
import com.finapse.entity.Account;
import com.finapse.entity.Transaction;
import com.finapse.enums.TransactionDirection;
import com.finapse.exception.ConflictException;
import com.finapse.exception.ResourceNotFoundException;
import com.finapse.repository.AccountRepository;
import com.finapse.repository.StatementRepository;
import com.finapse.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final StatementRepository statementRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<AccountResponse> getAll() {
        UUID userId = userService.getCurrentUserId();
        return accountRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getById(UUID id) {
        return AccountResponse.from(findOrThrow(id));
    }

    @Transactional
    public AccountResponse create(AccountCreateRequest request) {
        Account account = new Account();
        account.setUser(userService.getCurrentUser());
        account.setName(request.name());
        account.setInstitutionName(request.institutionName());
        account.setLastFourDigits(request.lastFourDigits());
        if (request.currency() != null) {
            account.setCurrency(request.currency().toUpperCase());
        }
        return AccountResponse.from(accountRepository.save(account));
    }

    @Transactional
    public AccountResponse deactivate(UUID id) {
        Account account = findOrThrow(id);
        account.setActive(false);
        return AccountResponse.from(accountRepository.save(account));
    }

    @Transactional
    public AccountResponse update(UUID id, AccountCreateRequest request) {
        Account account = findOrThrow(id);
        account.setName(request.name());
        account.setInstitutionName(request.institutionName());
        account.setLastFourDigits(request.lastFourDigits());
        if (request.currency() != null) {
            account.setCurrency(request.currency().toUpperCase());
        }
        return AccountResponse.from(accountRepository.save(account));
    }

    @Transactional
    public void delete(UUID id) {
        Account account = findOrThrow(id);
        if (statementRepository.existsByAccountId(id)) {
            throw new ConflictException("Cannot delete account because it has linked statements. Delete the statements first, or deactivate the account.");
        }
        accountRepository.delete(account);
    }

    @Transactional(readOnly = true)
    public AccountAnalyticsResponse getAnalytics(UUID accountId) {
        Account account = findOrThrow(accountId);
        List<Transaction> txs = transactionRepository.findByAccountIdOrderByTransactionDateDesc(accountId);

        BigDecimal inflow = sum(txs, TransactionDirection.CREDIT);
        BigDecimal outflow = sum(txs, TransactionDirection.DEBIT);
        BigDecimal netChange = inflow.subtract(outflow);

        return new AccountAnalyticsResponse(
                account.getId(), account.getName(),
                inflow, outflow, netChange,
                txs.size()
        );
    }

    private BigDecimal sum(List<Transaction> txs, TransactionDirection direction) {
        return txs.stream()
                .filter(t -> t.getDirection() == direction)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Package-visible for use by StatementService in Phase 4
    public Account findOrThrow(UUID id) {
        return accountRepository.findByIdAndUserId(id, userService.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found: " + id));
    }
}
