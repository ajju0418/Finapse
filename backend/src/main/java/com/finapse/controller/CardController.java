package com.finapse.controller;

import com.finapse.dto.CardAnalyticsResponse;
import com.finapse.dto.CardCreateRequest;
import com.finapse.dto.CardResponse;
import com.finapse.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @GetMapping
    public ResponseEntity<List<CardResponse>> getAll() {
        return ResponseEntity.ok(cardService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CardResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(cardService.getById(id));
    }

    @GetMapping("/{id}/analytics")
    public ResponseEntity<CardAnalyticsResponse> getAnalytics(@PathVariable UUID id) {
        return ResponseEntity.ok(cardService.getAnalytics(id));
    }

    @PostMapping
    public ResponseEntity<CardResponse> create(@Valid @RequestBody CardCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.create(request));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<CardResponse> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(cardService.deactivate(id));
    }
}
