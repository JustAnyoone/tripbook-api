package com.tt.tripbook.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Data required to create or update a destination")
public record DestinationRequest(

        @Schema(example = "Lisbon")
        @NotBlank(message = "city is required")
        String city,

        @Schema(example = "Portugal")
        @NotBlank(message = "country is required")
        String country,

        @Schema(example = "Portuguese capital, known for its trams and the Tagus river")
        String description) {
}
