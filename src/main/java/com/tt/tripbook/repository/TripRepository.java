package com.tt.tripbook.repository;

import com.tt.tripbook.model.Trip;
import com.tt.tripbook.model.TripStatus;
import java.util.Optional;
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

    // A trip belongs to its passenger, and the passenger to an account: every query below is scoped
    // through Trip.passenger.owner.id, so an account only ever sees its own trips.
    @EntityGraph(attributePaths = {"passenger", "destination"})
    Page<Trip> findByPassengerOwnerId(Long ownerId, Pageable pageable);

    @EntityGraph(attributePaths = {"passenger", "destination"})
    Page<Trip> findByPassengerOwnerIdAndStatus(Long ownerId, TripStatus status, Pageable pageable);

    // These two traverse the relationship: Trip.passenger.id and Trip.destination.id
    @EntityGraph(attributePaths = {"passenger", "destination"})
    Page<Trip> findByPassengerIdAndPassengerOwnerId(Long passengerId, Long ownerId, Pageable pageable);

    @EntityGraph(attributePaths = {"passenger", "destination"})
    Page<Trip> findByDestinationIdAndPassengerOwnerId(Long destinationId, Long ownerId, Pageable pageable);

    @EntityGraph(attributePaths = {"passenger", "destination"})
    Optional<Trip> findByIdAndPassengerOwnerId(Long id, Long ownerId);

    boolean existsByDestinationId(Long destinationId);
}
