package com.tt.tripbook.controller;

import com.tt.tripbook.dto.DestinationRequest;
import com.tt.tripbook.dto.DestinationResponse;
import com.tt.tripbook.dto.PageResponse;
import com.tt.tripbook.service.DestinationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/destinations")
@Tag(name = "destinations", description = "Manage travel destinations")
public class DestinationController {

    private final DestinationService service;

    public DestinationController(DestinationService service) {
        this.service = service;
    }

    @Operation(summary = "List destinations, paginated",
            description = "Optional filter by country (case-insensitive, partial match). "
                    + "Use page, size and sort (e.g. sort=city,desc) to control the result.")
    @ApiResponse(responseCode = "400", description = "Invalid sort property or parameter value")
    @GetMapping
    public PageResponse<DestinationResponse> findAll(
            @RequestParam(required = false) String country,
            @ParameterObject @PageableDefault(size = 10, sort = "city") Pageable pageable) {
        return service.findAll(country, pageable);
    }

    @Operation(summary = "Find a destination by id")
    @ApiResponse(responseCode = "200", description = "Destination found")
    @ApiResponse(responseCode = "404", description = "Destination not found")
    @GetMapping("/{id}")
    public DestinationResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @Operation(summary = "Create a destination (admin only)")
    @ApiResponse(responseCode = "201", description = "Destination created")
    @ApiResponse(responseCode = "400", description = "Invalid data")
    @ApiResponse(responseCode = "403", description = "Only admins can create destinations")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<DestinationResponse> create(@Valid @RequestBody DestinationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @Operation(summary = "Update a destination (admin only)")
    @ApiResponse(responseCode = "200", description = "Destination updated")
    @ApiResponse(responseCode = "400", description = "Invalid data")
    @ApiResponse(responseCode = "403", description = "Only admins can update destinations")
    @ApiResponse(responseCode = "404", description = "Destination not found")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public DestinationResponse update(@PathVariable Long id, @Valid @RequestBody DestinationRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "Delete a destination (admin only)")
    @ApiResponse(responseCode = "204", description = "Destination deleted")
    @ApiResponse(responseCode = "403", description = "Only admins can delete destinations")
    @ApiResponse(responseCode = "404", description = "Destination not found")
    @ApiResponse(responseCode = "409", description = "Destination has trips associated with it")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}