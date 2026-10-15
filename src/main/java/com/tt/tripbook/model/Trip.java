package com.tt.tripbook.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

@Schema(description = "Booking that links a passenger to a destination")
@Entity
@Table(name = "trip")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Schema(description = "Departure date", example = "2026-12-20")
    @NotNull(message = "the departure date is required")
    @Column(name = "departure_date", nullable = false)
    private LocalDate departureDate;

    @Schema(description = "Return date (optional)", example = "2026-12-30")
    @Column(name = "return_date")
    private LocalDate returnDate;

    @Schema(description = "Total price in dollars", example = "3500.50")
    @NotNull(message = "the price is required")
    @Positive(message = "the price must be greater than zero")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Schema(description = "Booking status", example = "CONFIRMED", allowableValues = {"PENDING", "CONFIRMED", "CANCELLED"})
    @NotBlank(message = "the status is required")
    @Pattern(regexp = "PENDING|CONFIRMED|CANCELLED",
            message = "the status must be PENDING, CONFIRMED or CANCELLED")
    @Column(nullable = false)
    private String status;

    @Schema(description = "Provide only the passenger id", example = "{\"id\": 1}")
    @NotNull(message = "the passenger is required")
    @ManyToOne(optional = false)
    @JoinColumn(name = "passenger_id")
    @JsonIgnoreProperties("trips")
    private Passenger passenger;

    @Schema(description = "Provide only the destination id", example = "{\"id\": 1}")
    @NotNull(message = "the destination is required")
    @ManyToOne(optional = false)
    @JoinColumn(name = "destination_id")
    @JsonIgnoreProperties("trips")
    private Destination destination;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(LocalDate departureDate) {
        this.departureDate = departureDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Passenger getPassenger() {
        return passenger;
    }

    public void setPassenger(Passenger passenger) {
        this.passenger = passenger;
    }

    public Destination getDestination() {
        return destination;
    }

    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    
}
