package com.finapse.controller;

import com.finapse.dto.DashboardResponse;
import com.finapse.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(
            @RequestParam(defaultValue = "THIS_MONTH") String period) {
        return ResponseEntity.ok(dashboardService.getDashboard(period));
    }
}
