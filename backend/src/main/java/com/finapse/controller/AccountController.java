package com.finapse.controller;

import com.finapse.dto.AccountCreateRequest;
import com.finapse.dto.AccountResponse;
import com.finapse.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAll() {
        return ResponseEntity.ok(accountService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(accountService.getById(id));
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody AccountCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.create(request));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<AccountResponse> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(accountService.deactivate(id));
    }
}
