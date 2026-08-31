package com.finapse.controller;

import com.finapse.dto.SubscriptionSummaryResponse;
import com.finapse.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    public ResponseEntity<SubscriptionSummaryResponse> getAll() {
        return ResponseEntity.ok(subscriptionService.getSubscriptions());
    }
}
