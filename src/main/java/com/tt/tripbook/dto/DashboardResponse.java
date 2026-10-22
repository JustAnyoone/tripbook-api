package com.tt.tripbook.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Summary of the signed-in account's trips. Cancelled trips are left out of totals, upcoming and top destinations.")
public record DashboardResponse(
        @Schema(description = "Trips of every status, counted per status") List<StatusCount> statusCounts,
        @Schema(description = "Money booked, one entry per currency (prices in different currencies are never added together)")
        List<CurrencyTotal> totals,
        @Schema(description = "The next trips that have not departed yet, soonest first") List<TripResponse> upcoming,
        @Schema(description = "The destinations with most trips") List<TopDestination> topDestinations) {

    public record StatusCount(String status, long trips) {
    }

    public record CurrencyTotal(String currency, long trips, BigDecimal total) {
    }

    public record TopDestination(String city, String country, long trips) {
    }
}
