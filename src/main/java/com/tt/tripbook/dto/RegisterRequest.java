package com.tt.tripbook.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Data required to create an account")
public record RegisterRequest(

        @Schema(example = "Ana Souza")
        @NotBlank(message = "name is required")
        String name,

        @Schema(example = "ana@email.com")
        @NotBlank(message = "email is required")
        @Email(message = "invalid email")
        String email,

        // 72 bytes is the most BCrypt reads
        @Schema(example = "a-long-passphrase", description = "8 to 72 characters")
        @NotBlank(message = "password is required")
        @Size(min = 8, max = 72, message = "password must have 8 to 72 characters")
        String password) {
}
