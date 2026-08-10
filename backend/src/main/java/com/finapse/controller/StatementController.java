package com.finapse.controller;

import com.finapse.dto.StatementResponse;
import com.finapse.enums.StatementType;
import com.finapse.service.StatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/statements")
@RequiredArgsConstructor
public class StatementController {

    private final StatementService statementService;

    @GetMapping
    public ResponseEntity<List<StatementResponse>> getAll() {
        return ResponseEntity.ok(statementService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StatementResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(statementService.getById(id));
    }

    @PostMapping("/upload")
    public ResponseEntity<StatementResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("statementType") StatementType statementType,
            @RequestParam(value = "accountId", required = false) UUID accountId,
            @RequestParam(value = "cardId",    required = false) UUID cardId) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(statementService.upload(file, statementType, accountId, cardId));
    }
}
