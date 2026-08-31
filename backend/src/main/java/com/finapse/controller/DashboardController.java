package com.finapse.controller;

import com.finapse.dto.DashboardResponse;
import com.finapse.dto.TrendResponse;
import com.finapse.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Supply {@code period=CUSTOM} together with {@code from} and {@code to}
     * (ISO dates) for an arbitrary range; otherwise a named period is used.
     */
    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(
            @RequestParam(defaultValue = "THIS_MONTH") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(dashboardService.getDashboard(period, from, to));
    }

    /** Monthly income / spending / net cash-flow series for the trend chart. */
    @GetMapping("/trends")
    public ResponseEntity<TrendResponse> getTrends(
            @RequestParam(defaultValue = "12") int months,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(dashboardService.getTrends(months, from, to));
    }
}
