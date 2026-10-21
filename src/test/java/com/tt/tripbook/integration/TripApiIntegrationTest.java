package com.tt.tripbook.integration;

import static com.tt.tripbook.TestData.destination;
import static com.tt.tripbook.TestData.passenger;
import static com.tt.tripbook.TestData.user;
import static com.tt.tripbook.TestData.trip;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tt.tripbook.model.Destination;
import com.tt.tripbook.model.Passenger;
import com.tt.tripbook.model.Trip;
import com.tt.tripbook.model.TripStatus;
import com.tt.tripbook.repository.DestinationRepository;
import com.tt.tripbook.repository.PassengerRepository;
import com.tt.tripbook.TestAuth;
import com.tt.tripbook.model.AppUser;
import com.tt.tripbook.repository.TripRepository;
import com.tt.tripbook.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

// End-to-end through the real stack: controller -> service -> repository -> H2.
// No @Transactional here on purpose: each HTTP call must run in its own transaction,
// exactly like in production. Data is cleaned before every test instead.
// MockMvc is built by hand from the WebApplicationContext, so the test does not depend
// on Spring Boot's MockMvc auto-configuration annotations (which moved in Boot 4).
@SpringBootTest
class TripApiIntegrationTest {

    // The date rules depend on "today": the clock is frozen so these tests do not expire
    @TestConfiguration
    static class FrozenClock {
        @Bean
        @Primary
        Clock testClock() {
            return Clock.fixed(Instant.parse("2026-10-17T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private DestinationRepository destinationRepository;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;

    // The signed-in account: the API only shows it the passengers and trips it owns
    private AppUser owner;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        // Children first, because of the foreign keys
        tripRepository.deleteAll();
        passengerRepository.deleteAll();
        destinationRepository.deleteAll();
        userRepository.deleteAll();
        owner = userRepository.save(user("owner@email.com"));
        TestAuth.signIn(owner);
    }

    // Leave the shared in-memory database empty for the test classes that run after this one
    @AfterEach
    void cleanUp() {
        TestAuth.signOut();
        tripRepository.deleteAll();
        passengerRepository.deleteAll();
        destinationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void postTrip_persistsTheTrip_andReturnsItWithPassengerAndDestination() throws Exception {
        Passenger ana = passengerRepository.save(passenger(owner, "AB123456"));
        Destination lisbon = destinationRepository.save(destination("Lisbon"));

        mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tripJson(ana.getId(), lisbon.getId(), "2026-12-30")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.departureDate").value("2026-12-20"))
                .andExpect(jsonPath("$.passenger.name").value("Ana Souza"))
                .andExpect(jsonPath("$.destination.city").value("Lisbon"));

        assertThat(tripRepository.count()).isEqualTo(1);
    }

    @Test
    void getTrips_showsOnlyTheTripsOfTheSignedInAccount() throws Exception {
        AppUser other = userRepository.save(user("other@email.com"));
        Passenger ana = passengerRepository.save(passenger(owner, "AB123456"));
        Passenger bob = passengerRepository.save(passenger(other, "CD789012", "Bob Stone"));
        Destination lisbon = destinationRepository.save(destination("Lisbon"));
        tripRepository.save(trip(ana, lisbon));
        tripRepository.save(trip(bob, lisbon));

        mockMvc.perform(get("/api/trips"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].passenger.name").value("Ana Souza"));
        // A shared destination only lists the trips of this account
        mockMvc.perform(get("/api/trips/destination/" + lisbon.getId()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getTrip_returns404_forATripOfAnotherAccount() throws Exception {
        AppUser other = userRepository.save(user("other@email.com"));
        Passenger bob = passengerRepository.save(passenger(other, "CD789012", "Bob Stone"));
        Destination lisbon = destinationRepository.save(destination("Lisbon"));
        Long othersTripId = tripRepository.save(trip(bob, lisbon)).getId();

        mockMvc.perform(get("/api/trips/" + othersTripId)).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/trips/" + othersTripId)).andExpect(status().isNotFound());
        assertThat(tripRepository.count()).isEqualTo(1);
    }

    @Test
    void postTrip_returns404_whenThePassengerBelongsToAnotherAccount() throws Exception {
        AppUser other = userRepository.save(user("other@email.com"));
        Passenger bob = passengerRepository.save(passenger(other, "CD789012", "Bob Stone"));
        Destination lisbon = destinationRepository.save(destination("Lisbon"));

        mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tripJson(bob.getId(), lisbon.getId(), "2026-12-30")))
                .andExpect(status().isNotFound());

        assertThat(tripRepository.count()).isZero();
    }

    @Test
    void postTrip_returns404_andSavesNothing_whenThePassengerDoesNotExist() throws Exception {
        Destination lisbon = destinationRepository.save(destination("Lisbon"));

        mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tripJson(999999L, lisbon.getId(), "2026-12-30")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Passenger not found: id 999999"));

        assertThat(tripRepository.count()).isZero();
    }

    @Test
    void postTrip_returns400_andSavesNothing_whenTheReturnDateIsBeforeTheDeparture() throws Exception {
        Passenger ana = passengerRepository.save(passenger(owner, "AB123456"));
        Destination lisbon = destinationRepository.save(destination("Lisbon"));

        mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tripJson(ana.getId(), lisbon.getId(), "2026-12-10")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Return date cannot be before departure date"));

        assertThat(tripRepository.count()).isZero();
    }

    @Test
    void postTrip_returns400_andSavesNothing_whenTheDepartureIsInThePast() throws Exception {
        Passenger ana = passengerRepository.save(passenger(owner, "AB123456"));
        Destination lisbon = destinationRepository.save(destination("Lisbon"));

        mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tripJson(ana.getId(), lisbon.getId(), "2026-12-30")
                                .replace("2026-12-20", "2026-10-16")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Departure date cannot be in the past"));

        assertThat(tripRepository.count()).isZero();
    }

    @Test
    void postTrip_returns400_whenTheDateDoesNotExistInTheCalendar() throws Exception {
        Passenger ana = passengerRepository.save(passenger(owner, "AB123456"));
        Destination lisbon = destinationRepository.save(destination("Lisbon"));

        mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tripJson(ana.getId(), lisbon.getId(), "2026-12-30")
                                .replace("2026-12-20", "2026-11-31")))
                .andExpect(status().isBadRequest());

        assertThat(tripRepository.count()).isZero();
    }

    @Test
    void getTripsByPassenger_returnsOnlyThatPassengersTrips() throws Exception {
        Passenger ana = passengerRepository.save(passenger(owner, "AB123456"));
        Passenger john = passengerRepository.save(passenger(owner, "CD789012"));
        Destination lisbon = destinationRepository.save(destination("Lisbon"));
        tripRepository.save(trip(ana, lisbon));
        tripRepository.save(trip(john, lisbon));

        mockMvc.perform(get("/api/trips/passenger/" + ana.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].passenger.id").value(ana.getId().intValue()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getTrips_filtersByStatus_andShowsTheNewestDepartureFirst() throws Exception {
        Passenger ana = passengerRepository.save(passenger(owner, "AB123456"));
        Destination lisbon = destinationRepository.save(destination("Lisbon"));
        Trip early = trip(ana, lisbon);
        Trip late = trip(ana, lisbon);
        late.setDepartureDate(java.time.LocalDate.of(2027, 1, 10));
        late.setReturnDate(java.time.LocalDate.of(2027, 1, 20));
        Trip cancelled = trip(ana, lisbon);
        cancelled.setStatus(TripStatus.CANCELLED);
        tripRepository.save(early);
        tripRepository.save(late);
        tripRepository.save(cancelled);

        // Lower case is accepted; both CONFIRMED trips come back, latest departure first
        mockMvc.perform(get("/api/trips").param("status", "confirmed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].departureDate").value("2027-01-10"))
                .andExpect(jsonPath("$.content[1].departureDate").value("2026-12-20"));
    }

    @Test
    void getTrips_returns400_whenTheStatusFilterIsUnknown() throws Exception {
        mockMvc.perform(get("/api/trips").param("status", "XYZ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("status must be PENDING, CONFIRMED or CANCELLED"));
    }

    @Test
    void getTrips_returnsTheRequestedPage() throws Exception {
        Passenger ana = passengerRepository.save(passenger(owner, "AB123456"));
        Destination lisbon = destinationRepository.save(destination("Lisbon"));
        for (int i = 0; i < 3; i++) {
            tripRepository.save(trip(ana, lisbon));
        }

        mockMvc.perform(get("/api/trips").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void getTripsByDestination_returnsOnlyTripsToThatDestination() throws Exception {
        Passenger ana = passengerRepository.save(passenger(owner, "AB123456"));
        Destination lisbon = destinationRepository.save(destination("Lisbon"));
        Destination tokyo = destinationRepository.save(destination("Tokyo", "Japan"));
        tripRepository.save(trip(ana, lisbon));
        tripRepository.save(trip(ana, tokyo));

        mockMvc.perform(get("/api/trips/destination/" + tokyo.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].destination.city").value("Tokyo"));
    }

    @Test
    void getTrip_returns404_withTheErrorBody_whenTheTripDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/trips/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Trip not found: id 999999"));
    }

    @Test
    void deleteDestination_returns409_whileItHasTrips_andSucceedsOnceTheyAreGone() throws Exception {
        Passenger ana = passengerRepository.save(passenger(owner, "AB123456"));
        Destination lisbon = destinationRepository.save(destination("Lisbon"));
        Trip savedTrip = tripRepository.save(trip(ana, lisbon));

        mockMvc.perform(delete("/api/destinations/" + lisbon.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot delete destination: it has trips associated with it"));

        mockMvc.perform(delete("/api/trips/" + savedTrip.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/destinations/" + lisbon.getId()))
                .andExpect(status().isNoContent());

        assertThat(destinationRepository.count()).isZero();
    }

    @Test
    void deletePassenger_returns204_andRemovesTheirTrips() throws Exception {
        Passenger ana = passengerRepository.save(passenger(owner, "AB123456"));
        Destination lisbon = destinationRepository.save(destination("Lisbon"));
        tripRepository.save(trip(ana, lisbon));

        mockMvc.perform(delete("/api/passengers/" + ana.getId()))
                .andExpect(status().isNoContent());

        assertThat(passengerRepository.count()).isZero();
        assertThat(tripRepository.count()).isZero();
        assertThat(destinationRepository.count()).isEqualTo(1);
    }

    @Test
    void postPassenger_returns409_whenTheDocumentNumberIsAlreadyRegistered() throws Exception {
        String body = """
                {
                  "name": "Ana Souza",
                  "documentNumber": "AB123456",
                  "email": "ana@email.com",
                  "birthDate": "1998-05-20"
                }
                """;

        mockMvc.perform(post("/api/passengers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/passengers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A passenger with this document number already exists"));

        assertThat(passengerRepository.count()).isEqualTo(1);
    }

    @Test
    void postDestination_ignoresAnIdSentInTheBody() throws Exception {
        mockMvc.perform(post("/api/destinations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": 50,
                                  "city": "Rome",
                                  "country": "Italy"
                                }
                                """))
                .andExpect(status().isCreated());

        assertThat(destinationRepository.findById(50L)).isEmpty();
        assertThat(destinationRepository.count()).isEqualTo(1);
    }

    private String tripJson(Long passengerId, Long destinationId, String returnDate) {
        return """
                {
                  "departureDate": "2026-12-20",
                  "returnDate": "%s",
                  "price": 3500.50,
                  "status": "CONFIRMED",
                  "passengerId": %d,
                  "destinationId": %d
                }
                """.formatted(returnDate, passengerId, destinationId);
    }
}
