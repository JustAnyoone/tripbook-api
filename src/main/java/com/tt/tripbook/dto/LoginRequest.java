package com.tt.tripbook.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credentials of an existing account")
public record LoginRequest(

        @Schema(example = "ana@email.com")
        @NotBlank(message = "email is required")
        String email,

        @Schema(example = "a-long-passphrase")
        @NotBlank(message = "password is required")
        String password) {
}
