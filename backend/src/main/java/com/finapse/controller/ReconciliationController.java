package com.finapse.controller;

import com.finapse.dto.ReconciliationDecisionRequest;
import com.finapse.dto.ReconciliationReviewResponse;
import com.finapse.service.ReconciliationReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reconciliation")
@RequiredArgsConstructor
public class ReconciliationController {

    private final ReconciliationReviewService reconciliationReviewService;

    @GetMapping("/reviews")
    public ResponseEntity<List<ReconciliationReviewResponse>> getPending() {
        return ResponseEntity.ok(reconciliationReviewService.getPending());
    }

    @GetMapping("/reviews/count")
    public ResponseEntity<Long> countPending() {
        return ResponseEntity.ok(reconciliationReviewService.countPending());
    }

    @PostMapping("/reviews/{id}/decide")
    public ResponseEntity<ReconciliationReviewResponse> decide(
            @PathVariable UUID id,
            @Valid @RequestBody ReconciliationDecisionRequest request) {
        return ResponseEntity.ok(reconciliationReviewService.decide(id, request));
    }
}
