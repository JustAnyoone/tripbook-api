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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

// Runs against the in-memory H2 database from src/test/resources/application.properties.
// @Transactional rolls every test back, so tests do not leak data into each other.
@SpringBootTest
@Transactional
class PassengerRepositoryTest {

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private DestinationRepository destinationRepository;

    @Autowired
    private TripRepository tripRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void existsByDocumentNumber_isTrueOnlyForStoredNumbers() {
        passengerRepository.save(passenger("AB123456"));

        assertThat(passengerRepository.existsByDocumentNumber("AB123456")).isTrue();
        assertThat(passengerRepository.existsByDocumentNumber("ZZ999999")).isFalse();
    }

    @Test
    void existsByDocumentNumberAndIdNot_ignoresThePassengerItself() {
        Passenger ana = passengerRepository.save(passenger("AB123456"));

        assertThat(passengerRepository.existsByDocumentNumberAndIdNot("AB123456", ana.getId())).isFalse();
        assertThat(passengerRepository.existsByDocumentNumberAndIdNot("AB123456", ana.getId() + 1)).isTrue();
    }

    @Test
    void save_isRejectedByTheDatabase_whenTheDocumentNumberIsDuplicated() {
        passengerRepository.saveAndFlush(passenger("AB123456"));

        assertThatThrownBy(() -> passengerRepository.saveAndFlush(passenger("AB123456")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingAPassenger_alsoDeletesTheirTrips_butKeepsTheDestination() {
        Passenger ana = passengerRepository.save(passenger("AB123456"));
        Destination lisbon = destinationRepository.save(destination("Lisbon"));
        Trip savedTrip = tripRepository.save(trip(ana, lisbon));
        // Clear the persistence context so the passenger is reloaded with its real trips list.
        // Without this, the in-memory list is empty and the cascade would not see the trip.
        flushAndClear();

        passengerRepository.deleteById(ana.getId());
        flushAndClear();

        assertThat(tripRepository.findById(savedTrip.getId())).isEmpty();
        assertThat(destinationRepository.findById(lisbon.getId())).isPresent();
    }

    @Test
    void removingATripFromThePassengersList_deletesItFromTheDatabase() {
        Passenger ana = passengerRepository.save(passenger("AB123456"));
        Destination lisbon = destinationRepository.save(destination("Lisbon"));
        Trip savedTrip = tripRepository.save(trip(ana, lisbon));
        flushAndClear();

        // orphanRemoval = true: a trip that leaves the list is deleted
        Passenger loaded = passengerRepository.findById(ana.getId()).orElseThrow();
        loaded.getTrips().clear();
        flushAndClear();

        assertThat(tripRepository.findById(savedTrip.getId())).isEmpty();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
