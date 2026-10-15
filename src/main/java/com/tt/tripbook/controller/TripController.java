package com.tt.tripbook.controller;

import com.tt.tripbook.model.Trip;
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
@Tag(name = "trips", description = "Trip bookings: link a Passenger to a Destination")
public class TripController {

    private final TripService service;

    public TripController(TripService service) {
        this.service = service;
    }

    @Operation(summary = "Lists all trips")
    @ApiResponse(responseCode = "200", description = "List of trips")
    @GetMapping
    public List<Trip> findAll() {
        return service.findAll();
    }

    @Operation(summary = "Finds a Trip by id")
    @ApiResponse(responseCode = "200", description = "Trip found")
    @ApiResponse(responseCode = "404", description = "Trip not found")
    @GetMapping("/{id}")
    public Trip findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @Operation(summary = "Lists the trips of a Passenger")
    @ApiResponse(responseCode = "200", description = "Passenger's trips")
    @ApiResponse(responseCode = "404", description = "Passenger not found")
    @GetMapping("/passenger/{passengerId}")
    public List<Trip> byPassenger(@PathVariable Long passengerId) {
        return service.findAllPassenger(passengerId);
    }

    @Operation(summary = "Lists the trips to a Destination")
    @ApiResponse(responseCode = "200", description = "Destination's trips")
    @ApiResponse(responseCode = "404", description = "Destination not found")
    @GetMapping("/destination/{destinationId}")
    public List<Trip> byDestination(@PathVariable Long destinationId) {
        return service.findAllDestination(destinationId);
    }

    @Operation(summary = "Creates a Trip",
               description = "Provide only the id of the Passenger and the Destination, for example: \"Passenger\": { \"id\": 1 }")
    @ApiResponse(responseCode = "201", description = "Trip created")
    @ApiResponse(responseCode = "400", description = "Invalid data or inconsistent dates")
    @ApiResponse(responseCode = "404", description = "Passenger or Destination not found")
    @PostMapping
    public ResponseEntity<Trip> create(@Valid @RequestBody Trip trip) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(trip));
    }

    @Operation(summary = "Updates an existing Trip")
    @ApiResponse(responseCode = "200", description = "Trip updated")
    @ApiResponse(responseCode = "400", description = "Invalid data or inconsistent dates")
    @ApiResponse(responseCode = "404", description = "Trip, Passenger or Destination not found")
    @PutMapping("/{id}")
    public Trip update(@PathVariable Long id, @Valid @RequestBody Trip trip) {
        return service.update(id, trip);
    }

    @Operation(summary = "Removes a Trip")
    @ApiResponse(responseCode = "204", description = "Trip removed")
    @ApiResponse(responseCode = "404", description = "Trip not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
