package com.tt.tripbook.integration;

import static com.tt.tripbook.TestData.destination;
import static com.tt.tripbook.TestData.passenger;
import static com.tt.tripbook.TestData.trip;
import static com.tt.tripbook.TestData.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tt.tripbook.TestAuth;
import com.tt.tripbook.model.AppUser;
import com.tt.tripbook.model.Currency;
import com.tt.tripbook.model.Destination;
import com.tt.tripbook.model.Passenger;
import com.tt.tripbook.model.Trip;
import com.tt.tripbook.model.TripStatus;
import com.tt.tripbook.repository.DestinationRepository;
import com.tt.tripbook.repository.PassengerRepository;
import com.tt.tripbook.repository.TripRepository;
import com.tt.tripbook.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

// The dashboard and the per-currency price sort, through the real stack against H2
@SpringBootTest
class DashboardApiIntegrationTest {

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
    private AppUser owner;
    private Passenger ana;
    private Destination lisbon;
    private Destination tokyo;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        clean();
        owner = userRepository.save(user("owner@email.com"));
        TestAuth.signIn(owner);
        ana = passengerRepository.save(passenger(owner, "AB123456"));
        lisbon = destinationRepository.save(destination("Lisbon"));
        tokyo = destinationRepository.save(destination("Tokyo", "Japan"));
    }

    @AfterEach
    void cleanUp() {
        TestAuth.signOut();
        clean();
    }

    private void clean() {
        tripRepository.deleteAll();
        passengerRepository.deleteAll();
        destinationRepository.deleteAll();
        userRepository.deleteAll();
    }

    private Trip save(Destination destination, String departure, String price, Currency currency, TripStatus status) {
        Trip trip = trip(ana, destination);
        trip.setDepartureDate(LocalDate.parse(departure));
        trip.setReturnDate(null);
        trip.setPrice(new BigDecimal(price));
        trip.setCurrency(currency);
        trip.setStatus(status);
        return tripRepository.save(trip);
    }

    @Test
    void dashboard_totalsPerCurrency_leavingOutCancelledTrips() throws Exception {
        save(lisbon, "2026-12-01", "1000.00", Currency.EUR, TripStatus.CONFIRMED);
        save(lisbon, "2026-12-10", "500.50", Currency.EUR, TripStatus.PENDING);
        save(tokyo, "2026-11-20", "2000.00", Currency.BRL, TripStatus.CONFIRMED);
        save(tokyo, "2026-11-25", "9999.00", Currency.BRL, TripStatus.CANCELLED);

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.length()").value(2))
                .andExpect(jsonPath("$.totals[?(@.currency=='EUR')].total").value(1500.50))
                .andExpect(jsonPath("$.totals[?(@.currency=='EUR')].trips").value(2))
                .andExpect(jsonPath("$.totals[?(@.currency=='BRL')].total").value(2000.00))
                .andExpect(jsonPath("$.statusCounts[?(@.status=='CANCELLED')].trips").value(1));
    }

    @Test
    void dashboard_upcomingAreFutureTripsSoonestFirst_andTopDestinationsAreCounted() throws Exception {
        save(lisbon, "2026-10-01", "100.00", Currency.EUR, TripStatus.CONFIRMED); // already departed
        save(lisbon, "2026-12-01", "100.00", Currency.EUR, TripStatus.CONFIRMED);
        save(tokyo, "2026-11-01", "100.00", Currency.EUR, TripStatus.CONFIRMED);
        save(tokyo, "2026-11-05", "100.00", Currency.EUR, TripStatus.CANCELLED); // cancelled: ignored

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(jsonPath("$.upcoming.length()").value(2))
                .andExpect(jsonPath("$.upcoming[0].destination.city").value("Tokyo"))
                .andExpect(jsonPath("$.upcoming[1].destination.city").value("Lisbon"))
                .andExpect(jsonPath("$.topDestinations[0].city").value("Lisbon"))
                .andExpect(jsonPath("$.topDestinations[0].trips").value(2));
    }

    @Test
    void dashboard_ignoresTheTripsOfOtherAccounts() throws Exception {
        AppUser other = userRepository.save(user("other@email.com"));
        Passenger bob = passengerRepository.save(passenger(other, "CD789012", "Bob Stone"));
        Trip foreign = trip(bob, lisbon);
        foreign.setDepartureDate(LocalDate.parse("2026-12-01"));
        tripRepository.save(foreign);

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.length()").value(0))
                .andExpect(jsonPath("$.upcoming.length()").value(0))
                .andExpect(jsonPath("$.statusCounts.length()").value(0));
    }

    @Test
    void sortByPrice_groupsByCurrencyFirst() throws Exception {
        save(lisbon, "2026-12-01", "900.00", Currency.USD, TripStatus.CONFIRMED);
        save(lisbon, "2026-12-02", "50.00", Currency.USD, TripStatus.CONFIRMED);
        save(lisbon, "2026-12-03", "9000.00", Currency.BRL, TripStatus.CONFIRMED);
        save(lisbon, "2026-12-04", "100.00", Currency.BRL, TripStatus.CONFIRMED);

        mockMvc.perform(get("/api/trips?sort=price,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].currency").value("BRL"))
                .andExpect(jsonPath("$.content[0].price").value(100.00))
                .andExpect(jsonPath("$.content[1].price").value(9000.00))
                .andExpect(jsonPath("$.content[2].currency").value("USD"))
                .andExpect(jsonPath("$.content[2].price").value(50.00));
    }
}
