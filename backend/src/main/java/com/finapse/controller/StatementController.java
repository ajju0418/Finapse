package com.finapse.controller;

import com.finapse.dto.ColumnMappingOverride;
import com.finapse.dto.StatementPreviewResponse;
import com.finapse.dto.StatementResponse;
import com.finapse.enums.StatementType;
import com.finapse.service.StatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    @PostMapping("/{id}/reclassify")
    public ResponseEntity<StatementResponse> reclassify(@PathVariable UUID id) {
        return ResponseEntity.ok(statementService.reclassify(id));
    }

    /**
     * Dry-run parse. Returns the detected column mapping and sample rows so the
     * user can confirm or remap columns before importing. Nothing is saved.
     */
    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StatementPreviewResponse> preview(
            @RequestParam("file") MultipartFile file,
            @RequestPart(value = "columnMapping", required = false) ColumnMappingOverride columnMapping) {
        return ResponseEntity.ok(statementService.preview(file, columnMapping));
    }

    /**
     * Queues the import. Responds 202 with the statement in PROCESSING; poll
     * {@code GET /api/statements/{id}} until the status changes.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StatementResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("statementType") StatementType statementType,
            @RequestParam(value = "accountId", required = false) UUID accountId,
            @RequestParam(value = "cardId",    required = false) UUID cardId,
            @RequestPart(value = "columnMapping", required = false) ColumnMappingOverride columnMapping) {

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(statementService.upload(file, statementType, accountId, cardId, columnMapping));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        statementService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
