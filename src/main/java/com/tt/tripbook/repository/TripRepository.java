package com.tt.tripbook.repository;

import com.tt.tripbook.model.Trip;
import com.tt.tripbook.model.TripStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepository extends JpaRepository<Trip, Long> {

    // Every trip is returned together with its passenger and destination, so the entity graph
    // loads them in the same query instead of two extra selects per row.

    @Override
    @EntityGraph(attributePaths = {"passenger", "destination"})
    Page<Trip> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"passenger", "destination"})
    Page<Trip> findByStatus(TripStatus status, Pageable pageable);

    // These two traverse the relationship: Trip.passenger.id and Trip.destination.id
    @EntityGraph(attributePaths = {"passenger", "destination"})
    Page<Trip> findByPassengerId(Long passengerId, Pageable pageable);

    @EntityGraph(attributePaths = {"passenger", "destination"})
    Page<Trip> findByDestinationId(Long destinationId, Pageable pageable);

    boolean existsByDestinationId(Long destinationId);
}
