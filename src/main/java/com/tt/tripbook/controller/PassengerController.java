package com.tt.tripbook.controller;

import com.tt.tripbook.dto.PassengerRequest;
import com.tt.tripbook.dto.PassengerResponse;
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
@Tag(name = "passengers", description = "Manage passengers")
public class PassengerController {

    private final PassengerService service;

    public PassengerController(PassengerService service) {
        this.service = service;
    }

    @Operation(summary = "List all passengers")
    @GetMapping
    public List<PassengerResponse> findAll() {
        return service.findAll();
    }

    @Operation(summary = "Find a passenger by id")
    @ApiResponse(responseCode = "200", description = "Passenger found")
    @ApiResponse(responseCode = "404", description = "Passenger not found")
    @GetMapping("/{id}")
    public PassengerResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @Operation(summary = "Register a passenger")
    @ApiResponse(responseCode = "201", description = "Passenger created")
    @ApiResponse(responseCode = "400", description = "Invalid data")
    @ApiResponse(responseCode = "409", description = "Document number already registered")
    @PostMapping
    public ResponseEntity<PassengerResponse> create(@Valid @RequestBody PassengerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @Operation(summary = "Update a passenger")
    @ApiResponse(responseCode = "200", description = "Passenger updated")
    @ApiResponse(responseCode = "400", description = "Invalid data")
    @ApiResponse(responseCode = "404", description = "Passenger not found")
    @ApiResponse(responseCode = "409", description = "Document number belongs to another passenger")
    @PutMapping("/{id}")
    public PassengerResponse update(@PathVariable Long id, @Valid @RequestBody PassengerRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "Delete a passenger and their trips")
    @ApiResponse(responseCode = "204", description = "Passenger deleted")
    @ApiResponse(responseCode = "404", description = "Passenger not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}