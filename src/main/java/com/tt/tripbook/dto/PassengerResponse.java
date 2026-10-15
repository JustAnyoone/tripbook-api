package com.tt.tripbook.dto;

import com.tt.tripbook.model.Passenger;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "A registered passenger")
public record PassengerResponse(Long id, String name, String documentNumber, String email, LocalDate birthDate) {

    public static PassengerResponse from(Passenger passenger) {
        return new PassengerResponse(
                passenger.getId(),
                passenger.getName(),
                passenger.getDocumentNumber(),
                passenger.getEmail(),
                passenger.getBirthDate());
    }
}