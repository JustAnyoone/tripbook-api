package com.tt.tripbook.controller;

import com.tt.tripbook.model.Passenger;
import com.tt.tripbook.service.PassengerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/passengers")
@Tag(name = "Passengers", description = "Registration and lookup of Passengers")
public class PassengerController {

    private final PassengerService service;

    public PassengerController(PassengerService service) {
        this.service = service;
    }

    @Operation(summary = "Lists all Passengers")
    @ApiResponse(responseCode = "200", description = "List of Passengers")
    @GetMapping
    public List<Passenger> findAll() {
        return service.findAll();
    }

    @Operation(summary = "Finds a Passenger by id")
    @ApiResponse(responseCode = "200", description = "Passenger found")
    @ApiResponse(responseCode = "404", description = "Passenger not found")
    @GetMapping("/{id}")
    public Passenger findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @Operation(summary = "Registers a new Passenger")
    @ApiResponse(responseCode = "201", description = "Passenger created")
    @ApiResponse(responseCode = "400", description = "Invalid data")
    @ApiResponse(responseCode = "409", description = "DocumentNumber already registered")
    @PostMapping
    public ResponseEntity<Passenger> create(@Valid @RequestBody Passenger passenger) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(passenger));
    }

    @Operation(summary = "Updates an existing Passenger")
    @ApiResponse(responseCode = "200", description = "Passenger updated")
    @ApiResponse(responseCode = "400", description = "Invalid data")
    @ApiResponse(responseCode = "404", description = "Passenger not found")
    @ApiResponse(responseCode = "409", description = "DocumentNumber already belongs to another Passenger")
    @PutMapping("/{id}")
    public Passenger update(@PathVariable Long id, @Valid @RequestBody Passenger passenger) {
        return service.update(id, passenger);
    }

    @Operation(summary = "Removes a Passenger and their trips")
    @ApiResponse(responseCode = "204", description = "Passenger removed")
    @ApiResponse(responseCode = "404", description = "Passenger not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
