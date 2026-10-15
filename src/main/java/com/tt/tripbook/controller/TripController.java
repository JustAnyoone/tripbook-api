package com.tt.tripbook.controller;

import com.tt.tripbook.dto.TripRequest;
import com.tt.tripbook.dto.TripResponse;
import com.tt.tripbook.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@Tag(name = "trips", description = "Trip bookings: each trip links one passenger to one destination")
public class TripController {

    private final TripService service;

    public TripController(TripService service) {
        this.service = service;
    }

    @Operation(summary = "List all trips")
    @GetMapping
    public List<TripResponse> findAll() {
        return service.findAll();
    }

    @Operation(summary = "Find a trip by id")
    @ApiResponse(responseCode = "200", description = "Trip found")
    @ApiResponse(responseCode = "404", description = "Trip not found")
    @GetMapping("/{id}")
    public TripResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @Operation(summary = "List the trips of a passenger")
    @ApiResponse(responseCode = "404", description = "Passenger not found")
    @GetMapping("/passenger/{passengerId}")
    public List<TripResponse> byPassenger(@PathVariable Long passengerId) {
        return service.findByPassenger(passengerId);
    }

    @Operation(summary = "List the trips to a destination")
    @ApiResponse(responseCode = "404", description = "Destination not found")
    @GetMapping("/destination/{destinationId}")
    public List<TripResponse> byDestination(@PathVariable Long destinationId) {
        return service.findByDestination(destinationId);
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