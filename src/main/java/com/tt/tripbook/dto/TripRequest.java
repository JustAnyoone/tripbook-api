package com.tt.tripbook.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Data required to create or update a trip")
public record TripRequest(

        @Schema(example = "2026-12-20")
        @NotNull(message = "departureDate is required")
        LocalDate departureDate,

        @Schema(example = "2026-12-30", description = "Optional")
        LocalDate returnDate,

        @Schema(example = "3500.50")
        @NotNull(message = "price is required")
        @Positive(message = "price must be greater than zero")
        BigDecimal price,

        @Schema(example = "CONFIRMED", allowableValues = {"PENDING", "CONFIRMED", "CANCELLED"})
        @NotBlank(message = "status is required")
        @Pattern(regexp = "PENDING|CONFIRMED|CANCELLED",
                 message = "status must be PENDING, CONFIRMED or CANCELLED")
        String status,

        @Schema(example = "1")
        @NotNull(message = "passengerId is required")
        Long passengerId,

        @Schema(example = "1")
        @NotNull(message = "destinationId is required")
        Long destinationId,

        @Schema(example = "BRL", allowableValues = {"BRL", "USD", "EUR"},
                description = "Optional. The currency of the price; BRL when omitted")
        @Pattern(regexp = "BRL|USD|EUR", message = "currency must be BRL, USD or EUR")
        String currency) {
}