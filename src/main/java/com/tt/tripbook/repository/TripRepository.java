package com.tt.tripbook.repository;

import com.tt.tripbook.model.Trip;
import com.tt.tripbook.model.TripStatus;
import com.tt.tripbook.model.Currency;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // Dashboard queries. Each one is scoped to the account through Trip.passenger.owner.id.

    record StatusRow(TripStatus status, Long trips) {
    }

    record CurrencyRow(Currency currency, Long trips, BigDecimal total) {
    }

    record DestinationRow(String city, String country, Long trips) {
    }

    @Query("select new com.tt.tripbook.repository.TripRepository$StatusRow(t.status, count(t)) "
            + "from Trip t where t.passenger.owner.id = :ownerId group by t.status")
    List<StatusRow> countByStatus(@Param("ownerId") Long ownerId);

    @Query("select new com.tt.tripbook.repository.TripRepository$CurrencyRow(t.currency, count(t), sum(t.price)) "
            + "from Trip t where t.passenger.owner.id = :ownerId and t.status <> :excluded "
            + "group by t.currency order by t.currency")
    List<CurrencyRow> totalsByCurrency(@Param("ownerId") Long ownerId, @Param("excluded") TripStatus excluded);

    @EntityGraph(attributePaths = {"passenger", "destination"})
    @Query("select t from Trip t where t.passenger.owner.id = :ownerId and t.status <> :excluded "
            + "and t.departureDate >= :from")
    List<Trip> findUpcoming(@Param("ownerId") Long ownerId, @Param("excluded") TripStatus excluded,
                            @Param("from") LocalDate from, Pageable pageable);

    @Query("select new com.tt.tripbook.repository.TripRepository$DestinationRow(d.city, d.country, count(t)) "
            + "from Trip t join t.destination d where t.passenger.owner.id = :ownerId and t.status <> :excluded "
            + "group by d.id, d.city, d.country order by count(t) desc, d.city")
    List<DestinationRow> topDestinations(@Param("ownerId") Long ownerId, @Param("excluded") TripStatus excluded,
                                         Pageable pageable);

    // Notification query: find CONFIRMED trips departing on a specific date, with passenger and destination loaded.
    @EntityGraph(attributePaths = {"passenger", "destination"})
    @Query("select t from Trip t where t.status = com.tt.tripbook.model.TripStatus.CONFIRMED "
            + "and t.departureDate = :date")
    List<Trip> findByStatusAndDepartureDateWithPassengerAndDestination(@Param("date") LocalDate date);
}
