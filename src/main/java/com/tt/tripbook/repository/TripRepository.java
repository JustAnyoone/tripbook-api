package com.tt.tripbook.repository;

import com.tt.tripbook.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {

    // Atravessam o relacionamento: Trip.Passenger.id e Trip.DestinationId.id
    List<Trip> findByPassengerId(Long passengerId);

    List<Trip> findByDestinationId(Long destinationId);

    boolean existsByDestinationId(Long destinationId);
}