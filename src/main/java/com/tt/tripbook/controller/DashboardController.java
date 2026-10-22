package com.tt.tripbook.controller;

import com.tt.tripbook.dto.DashboardResponse;
import com.tt.tripbook.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "dashboard", description = "Summary numbers for the signed-in account")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @Operation(summary = "Summary of the account's trips",
            description = "Trips per status, money booked per currency, the next departures and the most visited destinations.")
    @GetMapping
    public DashboardResponse summary() {
        return service.summary();
    }
}
