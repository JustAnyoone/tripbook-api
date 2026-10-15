package com.tt.tripbook.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

@Schema(description = "Data required to create or update a passenger")
public record PassengerRequest(

        @Schema(example = "Ana Souza")
        @NotBlank(message = "name is required")
        String name,

        @Schema(example = "AB123456", description = "6 to 20 letters or digits")
        @NotBlank(message = "document number is required")
        @Pattern(regexp = "[A-Za-z0-9]{6,20}", message = "document number must have 6 to 20 letters or digits")
        String documentNumber,

        @Schema(example = "ana@email.com")
        @NotBlank(message = "email is required")
        @Email(message = "invalid email")
        String email,

        @Schema(example = "1998-05-20")
        @Past(message = "birth date must be in the past")
        LocalDate birthDate) {
}