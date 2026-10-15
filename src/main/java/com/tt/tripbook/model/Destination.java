package com.tt.tripbook.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "destination")
public class Destination {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String country;

    private String description;

    // No cascade on removal: deleting a Destination must not delete other people's trips
    @OneToMany(mappedBy = "destination")
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
