package com.tt.tripbook.dto;

import com.tt.tripbook.model.Trip;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "A trip booking, with its passenger and destination")
public record TripResponse(
        Long id,
        LocalDate departureDate,
        LocalDate returnDate,
        BigDecimal price,
        String status,
        PassengerResponse passenger,
        DestinationResponse destination) {

    public static TripResponse from(Trip trip) {
        return new TripResponse(
                trip.getId(),
                trip.getDepartureDate(),
                trip.getReturnDate(),
                trip.getPrice(),
                trip.getStatus().name(),
                PassengerResponse.from(trip.getPassenger()),
                DestinationResponse.from(trip.getDestination()));
    }
}