package com.tt.tripbook.dto;

import com.tt.tripbook.model.Destination;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A travel destination")
public record DestinationResponse(Long id, String city, String country, String description) {

    // Entity -> DTO conversion lives next to the DTO so it stays in one place
    public static DestinationResponse from(Destination destination) {
        return new DestinationResponse(
                destination.getId(),
                destination.getCity(),
                destination.getCountry(),
                destination.getDescription());
    }
}
