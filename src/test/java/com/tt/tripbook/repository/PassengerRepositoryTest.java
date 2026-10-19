package com.tt.tripbook.repository;

import static com.tt.tripbook.TestData.destination;
import static com.tt.tripbook.TestData.passenger;
import static com.tt.tripbook.TestData.trip;
import static com.tt.tripbook.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tt.tripbook.model.AppUser;
import com.tt.tripbook.model.Destination;
import com.tt.tripbook.model.Passenger;
import com.tt.tripbook.model.Trip;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void existsByOwnerIdAndDocumentNumber_isScopedToTheOwner() {
        AppUser ana = userRepository.save(user("ana@email.com"));
        AppUser bob = userRepository.save(user("bob@email.com"));
        passengerRepository.save(passenger(ana, "AB123456"));

        assertThat(passengerRepository.existsByOwnerIdAndDocumentNumber(ana.getId(), "AB123456")).isTrue();
        assertThat(passengerRepository.existsByOwnerIdAndDocumentNumber(ana.getId(), "ZZ999999")).isFalse();
        // The same number under another account does not count
        assertThat(passengerRepository.existsByOwnerIdAndDocumentNumber(bob.getId(), "AB123456")).isFalse();
    }

    @Test
    void existsByOwnerIdAndDocumentNumberAndIdNot_ignoresThePassengerItself() {
        AppUser owner = userRepository.save(user("ana@email.com"));
        Passenger ana = passengerRepository.save(passenger(owner, "AB123456"));

        assertThat(passengerRepository.existsByOwnerIdAndDocumentNumberAndIdNot(owner.getId(), "AB123456", ana.getId())).isFalse();
        assertThat(passengerRepository.existsByOwnerIdAndDocumentNumberAndIdNot(owner.getId(), "AB123456", ana.getId() + 1)).isTrue();
    }

    @Test
    void findByOwnerIdAndNameContainingIgnoreCase_matchesPartialTextIgnoringCase_andPages() {
        AppUser owner = userRepository.save(user("ana@email.com"));
        passengerRepository.save(passenger(owner, "AB123456", "Ana Souza"));
        passengerRepository.save(passenger(owner, "CD789012", "Mariana Lima"));
        passengerRepository.save(passenger(owner, "EF345678", "John Smith"));

        Page<Passenger> firstPage = passengerRepository.findByOwnerIdAndNameContainingIgnoreCase(
                owner.getId(), "ANA", PageRequest.of(0, 1, Sort.by("name")));

        assertThat(firstPage.getContent()).extracting(Passenger::getName).containsExactly("Ana Souza");
        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.isLast()).isFalse();
    }

    @Test
    void findByOwnerId_returnsOnlyThePassengersOfThatAccount() {
        AppUser ana = userRepository.save(user("ana@email.com"));
        AppUser bob = userRepository.save(user("bob@email.com"));
        passengerRepository.save(passenger(ana, "AB123456", "Ana Souza"));
        passengerRepository.save(passenger(bob, "CD789012", "Bob Stone"));

        Page<Passenger> result = passengerRepository.findByOwnerId(ana.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Passenger::getName).containsExactly("Ana Souza");
    }

    @Test
    void findByIdAndOwnerId_doesNotFindAnotherAccountsPassenger() {
        AppUser ana = userRepository.save(user("ana@email.com"));
        AppUser bob = userRepository.save(user("bob@email.com"));
        Passenger anasPassenger = passengerRepository.save(passenger(ana, "AB123456"));

        assertThat(passengerRepository.findByIdAndOwnerId(anasPassenger.getId(), ana.getId())).isPresent();
        assertThat(passengerRepository.findByIdAndOwnerId(anasPassenger.getId(), bob.getId())).isEmpty();
    }

    @Test
    void save_isRejectedByTheDatabase_whenTheOwnerRepeatsADocumentNumber() {
        AppUser owner = userRepository.save(user("ana@email.com"));
        passengerRepository.saveAndFlush(passenger(owner, "AB123456"));

        assertThatThrownBy(() -> passengerRepository.saveAndFlush(passenger(owner, "AB123456")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void save_allowsTheSameDocumentNumberUnderTwoAccounts() {
        AppUser ana = userRepository.save(user("ana@email.com"));
        AppUser bob = userRepository.save(user("bob@email.com"));
        passengerRepository.saveAndFlush(passenger(ana, "AB123456"));

        passengerRepository.saveAndFlush(passenger(bob, "AB123456"));

        assertThat(passengerRepository.count()).isEqualTo(2);
    }

    @Test
    void assignOrphansTo_givesOwnerlessPassengersToTheAccount_andLeavesTheOthers() {
        AppUser ana = userRepository.save(user("ana@email.com"));
        AppUser bob = userRepository.save(user("bob@email.com"));
        Passenger orphan = passengerRepository.save(passenger("AB123456"));
        Passenger bobs = passengerRepository.save(passenger(bob, "CD789012"));
        flushAndClear();

        int changed = passengerRepository.assignOrphansTo(ana);
        flushAndClear();

        assertThat(changed).isEqualTo(1);
        assertThat(passengerRepository.findById(orphan.getId()).orElseThrow().getOwner().getId()).isEqualTo(ana.getId());
        assertThat(passengerRepository.findById(bobs.getId()).orElseThrow().getOwner().getId()).isEqualTo(bob.getId());
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
