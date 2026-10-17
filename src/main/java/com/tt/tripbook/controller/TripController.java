package com.tt.tripbook.controller;

import com.tt.tripbook.dto.PageResponse;
import com.tt.tripbook.dto.TripRequest;
import com.tt.tripbook.dto.TripResponse;
import com.tt.tripbook.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trips")
@Tag(name = "trips", description = "Trip bookings: each trip links one passenger to one destination")
public class TripController {

    private final TripService service;

    public TripController(TripService service) {
        this.service = service;
    }

    @Operation(summary = "List trips, paginated",
            description = "Optional filter by status (PENDING, CONFIRMED or CANCELLED, case-insensitive). "
                    + "Newest departure first by default; use page, size and sort to change it.")
    @ApiResponse(responseCode = "400", description = "Unknown status, invalid sort property or parameter value")
    @GetMapping
    public PageResponse<TripResponse> findAll(
            @RequestParam(required = false) String status,
            @ParameterObject @PageableDefault(size = 10, sort = "departureDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return service.findAll(status, pageable);
    }

    @Operation(summary = "Find a trip by id")
    @ApiResponse(responseCode = "200", description = "Trip found")
    @ApiResponse(responseCode = "404", description = "Trip not found")
    @GetMapping("/{id}")
    public TripResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @Operation(summary = "List the trips of a passenger, paginated")
    @ApiResponse(responseCode = "404", description = "Passenger not found")
    @GetMapping("/passenger/{passengerId}")
    public PageResponse<TripResponse> byPassenger(
            @PathVariable Long passengerId,
            @ParameterObject @PageableDefault(size = 10, sort = "departureDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return service.findByPassenger(passengerId, pageable);
    }

    @Operation(summary = "List the trips to a destination, paginated")
    @ApiResponse(responseCode = "404", description = "Destination not found")
    @GetMapping("/destination/{destinationId}")
    public PageResponse<TripResponse> byDestination(
            @PathVariable Long destinationId,
            @ParameterObject @PageableDefault(size = 10, sort = "departureDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return service.findByDestination(destinationId, pageable);
    }

    @Operation(summary = "Create a trip", description = "Send only the ids of the passenger and the destination")
    @ApiResponse(responseCode = "201", description = "Trip created")
    @ApiResponse(responseCode = "400", description = "Invalid data or inconsistent dates")
    @ApiResponse(responseCode = "404", description = "Passenger or destination not found")
    @PostMapping
    public ResponseEntity<TripResponse> create(@Valid @RequestBody TripRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @Operation(summary = "Update a trip")
    @ApiResponse(responseCode = "200", description = "Trip updated")
    @ApiResponse(responseCode = "400", description = "Invalid data or inconsistent dates")
    @ApiResponse(responseCode = "404", description = "Trip, passenger or destination not found")
    @PutMapping("/{id}")
    public TripResponse update(@PathVariable Long id, @Valid @RequestBody TripRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "Delete a trip")
    @ApiResponse(responseCode = "204", description = "Trip deleted")
    @ApiResponse(responseCode = "404", description = "Trip not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}