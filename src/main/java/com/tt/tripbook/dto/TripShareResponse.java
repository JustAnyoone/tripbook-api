package com.tt.tripbook.dto;

import com.tt.tripbook.model.TripShare;
import java.time.LocalDateTime;

public record TripShareResponse(
        Long id,
        Long tripId,
        UserResponse owner,
        UserResponse guest,
        LocalDateTime sharedAt
) {
    public static TripShareResponse from(TripShare share) {
        return new TripShareResponse(
                share.getId(),
                share.getTrip().getId(),
                UserResponse.from(share.getOwner()),
                UserResponse.from(share.getGuest()),
                share.getSharedAt()
        );
    }
}
