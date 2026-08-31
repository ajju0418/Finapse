package com.finapse.controller;

import com.finapse.dto.ManualTransactionRequest;
import com.finapse.dto.TransactionCorrectionRequest;
import com.finapse.dto.TransactionResponse;
import com.finapse.enums.TransactionType;
import com.finapse.service.ManualTransactionService;
import com.finapse.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final ManualTransactionService manualTransactionService;

    /** Records cash or other off-statement spending. */
    @PostMapping
    public ResponseEntity<TransactionResponse> createManual(
            @Valid @RequestBody ManualTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(manualTransactionService.create(request));
    }

    /** Deletes a manually added transaction. Imported rows are immutable. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteManual(@PathVariable UUID id) {
        manualTransactionService.delete(id);
        return ResponseEntity.noContent().build();
    }

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

    /**
     * Correct a transaction's classification and, by default, teach the engine
     * so future imports of the same narration are classified this way.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<TransactionResponse> correct(
            @PathVariable UUID id,
            @Valid @RequestBody TransactionCorrectionRequest request) {
        return ResponseEntity.ok(transactionService.applyCorrection(id, request));
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
