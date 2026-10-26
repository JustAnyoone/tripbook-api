package com.tt.tripbook.integration;

import static com.tt.tripbook.TestData.destination;
import static com.tt.tripbook.TestData.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tt.tripbook.TestAuth;
import com.tt.tripbook.model.AppUser;
import com.tt.tripbook.model.Role;
import com.tt.tripbook.repository.DestinationRepository;
import com.tt.tripbook.repository.PassengerRepository;
import com.tt.tripbook.repository.TripRepository;
import com.tt.tripbook.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

// Pagination, filtering and sorting through the real stack. This is also the proof that Spring
// resolves ?page=&size=&sort= into a Pageable in the real application context.
@SpringBootTest
class DestinationApiIntegrationTest {

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

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        tripRepository.deleteAll();
        passengerRepository.deleteAll();
        destinationRepository.deleteAll();
        userRepository.deleteAll();
        // Creating destinations is an admin-only action
        AppUser admin = user("admin@email.com");
        admin.setRole(Role.ADMIN);
        TestAuth.signIn(userRepository.save(admin));
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
    void getDestinations_returnsTheRequestedPage_withTheMetadata() throws Exception {
        saveFiveCities();

        // Default sort is city ascending: Berlin, Lisbon, Madrid, Paris, Rome -> page 1 = Madrid, Paris
        mockMvc.perform(get("/api/destinations").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].city").value("Madrid"))
                .andExpect(jsonPath("$.content[1].city").value("Paris"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void getDestinations_usesPageSizeTen_whenNothingIsSent() throws Exception {
        for (int i = 1; i <= 12; i++) {
            destinationRepository.save(destination(String.format("City %02d", i), "Spain"));
        }

        mockMvc.perform(get("/api/destinations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.totalElements").value(12))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void getDestinations_filtersByCountry_ignoringCase() throws Exception {
        saveFiveCities();

        mockMvc.perform(get("/api/destinations").param("country", "spa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].city").value("Madrid"));
    }

    @Test
    void getDestinations_sortsAsRequested() throws Exception {
        saveFiveCities();

        mockMvc.perform(get("/api/destinations").param("sort", "city,desc").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].city").value("Rome"));
    }

    @Test
    void getDestinations_returns400_whenTheSortPropertyDoesNotExist() throws Exception {
        saveFiveCities();

        mockMvc.perform(get("/api/destinations").param("sort", "banana"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid sort property: 'banana'"));
    }

    @Test
    void getDestinations_capsThePageSizeAtOneHundred() throws Exception {
        saveFiveCities();

        mockMvc.perform(get("/api/destinations").param("size", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void postDestination_returns409_whenTheSameCityAndCountryAlreadyExist() throws Exception {
        String body = "{\"city\":\"Rio de Janeiro\",\"country\":\"Brazil\",\"description\":\"Beaches\"}";
        mockMvc.perform(post("/api/destinations").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        // Same place with different letter case and stray spaces is still a duplicate
        String duplicate = "{\"city\":\" rio de janeiro \",\"country\":\"BRAZIL\"}";
        mockMvc.perform(post("/api/destinations").contentType(MediaType.APPLICATION_JSON).content(duplicate))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Destination already exists: rio de janeiro, BRAZIL"));
    }

    private void saveFiveCities() {
        destinationRepository.save(destination("Lisbon", "Portugal"));
        destinationRepository.save(destination("Madrid", "Spain"));
        destinationRepository.save(destination("Paris", "France"));
        destinationRepository.save(destination("Berlin", "Germany"));
        destinationRepository.save(destination("Rome", "Italy"));
    }
}
