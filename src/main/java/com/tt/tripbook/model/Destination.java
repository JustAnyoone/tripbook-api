package com.tt.tripbook.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

@Schema(description = "Destination location of a Trip")
@Entity
@Table(name = "destination")
public class Destination {

    @Schema(description = "Automatically generated identifier", accessMode = Schema.AccessMode.READ_ONLY)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Schema(description = "Name of the city", example = "Lisbon")
    @NotBlank(message = "the city is required")
    @Column(nullable = false)
    private String city;

    @Schema(description = "Country of the Destination", example = "Portugal")
    @NotBlank(message = "the country is required")
    @Column(nullable = false)
    private String country;

    @Schema(description = "Free text about the Destination", example = "Portuguese capital, known for its trams and the Tagus river")
    private String description;

    // No cascade on removal: deleting a Destination must not delete other people's trips
    @Schema(description = "Trips associated with this Destination", accessMode = Schema.AccessMode.READ_ONLY)
    @OneToMany(mappedBy = "destination")
    @JsonIgnoreProperties("destination")
    private List<Trip> trips = new ArrayList<>();

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getCity() {
        return city;
    }
    public void setCity(String city) {
        this.city = city;
    }
    public String getCountry() {
        return country;
    }
    public void setCountry(String country) {
        this.country = country;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public List<Trip> getTrips() {
        return trips;
    }
    public void setTrips(List<Trip> trips) {
        this.trips = trips;
    }

    
}
