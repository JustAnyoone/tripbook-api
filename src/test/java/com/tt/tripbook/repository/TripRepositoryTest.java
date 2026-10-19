package com.tt.tripbook.repository;

import static com.tt.tripbook.TestData.destination;
import static com.tt.tripbook.TestData.passenger;
import static com.tt.tripbook.TestData.trip;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tt.tripbook.model.Destination;
import com.tt.tripbook.model.Passenger;
import com.tt.tripbook.model.Trip;
import com.tt.tripbook.model.TripStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class TripRepositoryTest {

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private DestinationRepository destinationRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void findByPassengerId_returnsOnlyThatPassengersTrips() {
        Passenger ana = passengerRepository.save(passenger("AB123456"));
        Passenger john = passengerRepository.save(passenger("CD789012"));
        Destination lisbon = destinationRepository.save(destination("Lisbon"));
        Trip anasTrip = tripRepository.save(trip(ana, lisbon));
        tripRepository.save(trip(john, lisbon));

        Page<Trip> result = tripRepository.findByPassengerId(ana.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Trip::getId).containsExactly(anasTrip.getId());
    }

    @Test
    void findByDestinationId_returnsOnlyTripsToThatDestination() {
        Passenger ana = passengerRepository.save(passenger("AB123456"));
        Destination lisbon = destinationRepository.save(destination("Lisbon"));
        Destination tokyo = destinationRepository.save(destination("Tokyo"));
        tripRepository.save(trip(ana, lisbon));
        Trip toTokyo = tripRepository.save(trip(ana, tokyo));

        Page<Trip> result = tripRepository.findByDestinationId(tokyo.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Trip::getId).containsExactly(toTokyo.getId());
    }

    @Test
    void findByStatus_returnsOnlyThatStatus_sortedAndPaged() {
        Passenger ana = passengerRepository.save(passenger("AB123456"));
        Destination lisbon = destinationRepository.save(destination("Lisbon"));
        Trip pending = trip(ana, lisbon);
        pending.setStatus(TripStatus.PENDING);
        tripRepository.save(pending);
        Trip early = tripRepository.save(trip(ana, lisbon));
        Trip late = trip(ana, lisbon);
        late.setDepartureDate(LocalDate.of(2027, 1, 10));
        late.setReturnDate(LocalDate.of(2027, 1, 20));
        late = tripRepository.save(late);

        Page<Trip> result = tripRepository.findByStatus(
                TripStatus.CONFIRMED, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "departureDate")));

        assertThat(result.getContent()).extracting(Trip::getId).containsExactly(late.getId(), early.getId());
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void findAll_returnsTheRequestedPage_withPassengerAndDestinationLoaded() {
        Passenger ana = passengerRepository.save(passenger("AB123456"));
        Destination lisbon = destinationRepository.save(destination("Lisbon"));
        for (int i = 0; i < 3; i++) {
            tripRepository.save(trip(ana, lisbon));
        }
        entityManager.flush();
        entityManager.clear();

        Page<Trip> secondPage = tripRepository.findAll(PageRequest.of(1, 2));

        assertThat(secondPage.getContent()).hasSize(1);
        assertThat(secondPage.getTotalElements()).isEqualTo(3);
        assertThat(secondPage.getTotalPages()).isEqualTo(2);
        assertThat(secondPage.getContent().get(0).getPassenger().getName()).isEqualTo("Ana Souza");
    }

    @Test
    void existsByDestinationId_isTrueOnlyWhileATripUsesTheDestination() {
        Passenger ana = passengerRepository.save(passenger("AB123456"));
        Destination lisbon = destinationRepository.save(destination("Lisbon"));
        Destination tokyo = destinationRepository.save(destination("Tokyo"));
        Trip savedTrip = tripRepository.save(trip(ana, lisbon));

        assertThat(tripRepository.existsByDestinationId(lisbon.getId())).isTrue();
        assertThat(tripRepository.existsByDestinationId(tokyo.getId())).isFalse();

        tripRepository.delete(savedTrip);

        assertThat(tripRepository.existsByDestinationId(lisbon.getId())).isFalse();
    }

    @Test
    void deletingADestinationInUse_isRejectedByTheForeignKey() {
        Passenger ana = passengerRepository.save(passenger("AB123456"));
        Destination lisbon = destinationRepository.save(destination("Lisbon"));
        tripRepository.save(trip(ana, lisbon));
        entityManager.flush();
        entityManager.clear();

        // The database is the last line of defense, even if the service check were removed
        assertThatThrownBy(() -> {
            destinationRepository.deleteById(lisbon.getId());
            destinationRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
