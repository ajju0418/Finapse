package com.finapse.controller;

import com.finapse.dto.TransactionResponse;
import com.finapse.enums.TransactionType;
import com.finapse.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/statement/{statementId}")
    public ResponseEntity<List<TransactionResponse>> getByStatement(@PathVariable UUID statementId) {
        return ResponseEntity.ok(transactionService.getByStatement(statementId));
    }

    @GetMapping("/card/{cardId}")
    public ResponseEntity<List<TransactionResponse>> getByCard(@PathVariable UUID cardId) {
        return ResponseEntity.ok(transactionService.getByCard(cardId));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<TransactionResponse>> getByAccount(@PathVariable UUID accountId) {
        return ResponseEntity.ok(transactionService.getByAccount(accountId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.getById(id));
    }

    @PatchMapping("/{id}/type")
    public ResponseEntity<TransactionResponse> updateType(
            @PathVariable UUID id,
            @RequestParam TransactionType type) {
        return ResponseEntity.ok(transactionService.updateType(id, type));
    }

    @PatchMapping("/{id}/category")
    public ResponseEntity<TransactionResponse> updateCategory(
            @PathVariable UUID id,
            @RequestParam UUID categoryId) {
        return ResponseEntity.ok(transactionService.updateCategory(id, categoryId));
    }
}
