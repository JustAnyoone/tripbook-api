package com.tt.tripbook.controller;

import com.tt.tripbook.model.Destination;
import com.tt.tripbook.service.DestinationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/destinations")
@Tag(name = "destinations", description = "Registration and lookup of Trip Destinations")
public class DestinationController {

    private final DestinationService service;

    public DestinationController(DestinationService service) {
        this.service = service;
    }

    @Operation(summary = "Lists all Destinations")
    @ApiResponse(responseCode = "200", description = "List of Destinations")
    @GetMapping
    public List<Destination> findAll() {
        return service.findAll();
    }

    @Operation(summary = "Retrieve a Destination by its ID")
    @ApiResponse(responseCode = "200", description = "Destination found")
    @ApiResponse(responseCode = "404", description = "Destination not found")
    @GetMapping("/{id}")
    public Destination findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @Operation(summary = "Create a new Destination")
    @ApiResponse(responseCode = "201", description = "Destination created")
    @PostMapping
    public ResponseEntity<Destination> create(@Valid @RequestBody Destination destination) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(destination));
    }

    @Operation(summary = "Updates an existing Destination")
    @ApiResponse(responseCode = "200", description = "Destination updates")
    @ApiResponse(responseCode = "404", description = "Destination not found")
    @PutMapping("/{id}")
    public Destination update(@PathVariable Long id, @Valid @RequestBody Destination destination) {
        return service.update(id, destination);
    }

    @Operation(summary = "Removes a Destination")
    @ApiResponse(responseCode = "204", description = "Destination remove")
    @ApiResponse(responseCode = "404", description = "Destination not found")
    @ApiResponse(responseCode = "409", description = "Destination has associated trips")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
