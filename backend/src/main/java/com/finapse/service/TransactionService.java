package com.finapse.service;

import com.finapse.dto.TransactionResponse;
import com.finapse.entity.Category;
import com.finapse.entity.Transaction;
import com.finapse.enums.TransactionType;
import com.finapse.exception.ResourceNotFoundException;
import com.finapse.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<TransactionResponse> getByStatement(UUID statementId) {
        return transactionRepository.findByStatementIdOrderByTransactionDateDesc(statementId)
                .stream().map(TransactionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getByCard(UUID cardId) {
        return transactionRepository.findByCardIdOrderByTransactionDateDesc(cardId)
                .stream().map(TransactionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getByAccount(UUID accountId) {
        return transactionRepository.findByAccountIdOrderByTransactionDateDesc(accountId)
                .stream().map(TransactionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(UUID id) {
        return TransactionResponse.from(findOrThrow(id));
    }

    @Transactional
    public TransactionResponse updateType(UUID id, TransactionType type) {
        Transaction tx = findOrThrow(id);
        tx.setTransactionType(type);
        return TransactionResponse.from(transactionRepository.save(tx));
    }

    @Transactional
    public TransactionResponse updateCategory(UUID id, UUID categoryId) {
        Transaction tx = findOrThrow(id);
        Category category = categoryService.findOrThrow(categoryId);
        tx.setCategory(category);
        return TransactionResponse.from(transactionRepository.save(tx));
    }

    public Transaction findOrThrow(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));
    }
}
