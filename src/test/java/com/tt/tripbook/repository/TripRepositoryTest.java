package com.tt.tripbook.repository;

import static com.tt.tripbook.TestData.destination;
import static com.tt.tripbook.TestData.passenger;
import static com.tt.tripbook.TestData.trip;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tt.tripbook.model.Destination;
import com.tt.tripbook.model.Passenger;
import com.tt.tripbook.model.Trip;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
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

        List<Trip> result = tripRepository.findByPassengerId(ana.getId());

        assertThat(result).extracting(Trip::getId).containsExactly(anasTrip.getId());
    }

    @Test
    void findByDestinationId_returnsOnlyTripsToThatDestination() {
        Passenger ana = passengerRepository.save(passenger("AB123456"));
        Destination lisbon = destinationRepository.save(destination("Lisbon"));
        Destination tokyo = destinationRepository.save(destination("Tokyo"));
        tripRepository.save(trip(ana, lisbon));
        Trip toTokyo = tripRepository.save(trip(ana, tokyo));

        List<Trip> result = tripRepository.findByDestinationId(tokyo.getId());

        assertThat(result).extracting(Trip::getId).containsExactly(toTokyo.getId());
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
