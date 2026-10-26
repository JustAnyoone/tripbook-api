package com.tt.tripbook.controller;

import com.tt.tripbook.dto.TripResponse;
import com.tt.tripbook.dto.TripShareResponse;
import com.tt.tripbook.service.TripShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trips")
@Tag(name = "trip-shares", description = "Share trips with other users")
public class TripShareController {

    private final TripShareService service;

    public TripShareController(TripShareService service) {
        this.service = service;
    }

    @Operation(summary = "Share a trip with another user by email")
    @ApiResponse(responseCode = "201", description = "Trip shared")
    @ApiResponse(responseCode = "400", description = "Cannot share with yourself")
    @ApiResponse(responseCode = "404", description = "Trip or user not found")
    @ApiResponse(responseCode = "409", description = "Already shared with that user")
    @PostMapping("/{tripId}/shares")
    public ResponseEntity<TripShareResponse> share(
            @PathVariable Long tripId,
            @RequestParam String guestEmail) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.share(tripId, guestEmail));
    }

    @Operation(summary = "Remove a share (unshare trip with a user)")
    @ApiResponse(responseCode = "204", description = "Share removed")
    @ApiResponse(responseCode = "404", description = "Trip or share not found")
    @DeleteMapping("/{tripId}/shares/{guestId}")
    public ResponseEntity<Void> unshare(
            @PathVariable Long tripId,
            @PathVariable Long guestId) {
        service.unshare(tripId, guestId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List all users this trip is shared with")
    @ApiResponse(responseCode = "200", description = "List of shares")
    @GetMapping("/{tripId}/shares")
    public List<TripShareResponse> listSharesForTrip(@PathVariable Long tripId) {
        return service.listSharesForTrip(tripId);
    }

    @Operation(summary = "List all trips shared with me (from other accounts)")
    @ApiResponse(responseCode = "200", description = "Trips from other users shared with me")
    @GetMapping("/shared-with-me")
    public List<TripResponse> sharedWithMe() {
        return service.listSharedWithMe();
    }
}
