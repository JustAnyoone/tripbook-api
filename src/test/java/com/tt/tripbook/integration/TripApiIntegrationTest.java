package com.tt.tripbook.integration;

import static com.tt.tripbook.TestData.destination;
import static com.tt.tripbook.TestData.passenger;
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
import com.tt.tripbook.repository.DestinationRepository;
import com.tt.tripbook.repository.PassengerRepository;
import com.tt.tripbook.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private DestinationRepository destinationRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        // Children first, because of the foreign keys
        tripRepository.deleteAll();
        passengerRepository.deleteAll();
        destinationRepository.deleteAll();
    }

    @Test
    void postTrip_persistsTheTrip_andReturnsItWithPassengerAndDestination() throws Exception {
        Passenger ana = passengerRepository.save(passenger("AB123456"));
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
        Passenger ana = passengerRepository.save(passenger("AB123456"));
        Destination lisbon = destinationRepository.save(destination("Lisbon"));

        mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tripJson(ana.getId(), lisbon.getId(), "2026-12-10")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Return date cannot be before departure date"));

        assertThat(tripRepository.count()).isZero();
    }

    @Test
    void getTripsByPassenger_returnsOnlyThatPassengersTrips() throws Exception {
        Passenger ana = passengerRepository.save(passenger("AB123456"));
        Passenger john = passengerRepository.save(passenger("CD789012"));
        Destination lisbon = destinationRepository.save(destination("Lisbon"));
        tripRepository.save(trip(ana, lisbon));
        tripRepository.save(trip(john, lisbon));

        mockMvc.perform(get("/api/trips/passenger/" + ana.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].passenger.id").value(ana.getId().intValue()));
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
        Passenger ana = passengerRepository.save(passenger("AB123456"));
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
        Passenger ana = passengerRepository.save(passenger("AB123456"));
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
