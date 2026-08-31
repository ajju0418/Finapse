package com.finapse.controller;

import com.finapse.dto.LearnedRuleResponse;
import com.finapse.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * The classifier's learned memory — rules derived from user corrections.
 */
@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
public class ClassificationRuleController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<List<LearnedRuleResponse>> getAll() {
        return ResponseEntity.ok(transactionService.getLearnedRules());
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<Void> forget(@PathVariable UUID ruleId) {
        transactionService.forgetRule(ruleId);
        return ResponseEntity.noContent().build();
    }
}
