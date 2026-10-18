package com.tt.tripbook.integration;

import static com.tt.tripbook.TestData.passenger;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tt.tripbook.repository.DestinationRepository;
import com.tt.tripbook.repository.PassengerRepository;
import com.tt.tripbook.repository.TripRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

// Pagination and filtering of passengers through the real stack and the in-memory H2 database.
@SpringBootTest
class PassengerApiIntegrationTest {

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
        tripRepository.deleteAll();
        passengerRepository.deleteAll();
        destinationRepository.deleteAll();
    }

    // Leave the shared in-memory database empty for the test classes that run after this one
    @AfterEach
    void cleanUp() {
        tripRepository.deleteAll();
        passengerRepository.deleteAll();
        destinationRepository.deleteAll();
    }

    @Test
    void getPassengers_isSortedByNameAscending_byDefault() throws Exception {
        passengerRepository.save(passenger("AB123456", "Carla Dias"));
        passengerRepository.save(passenger("CD789012", "Ana Souza"));
        passengerRepository.save(passenger("EF345678", "Bruno Lima"));

        mockMvc.perform(get("/api/passengers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Ana Souza"))
                .andExpect(jsonPath("$.content[1].name").value("Bruno Lima"))
                .andExpect(jsonPath("$.content[2].name").value("Carla Dias"))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void getPassengers_filtersByName_ignoringCase() throws Exception {
        passengerRepository.save(passenger("AB123456", "Ana Souza"));
        passengerRepository.save(passenger("CD789012", "Mariana Lima"));
        passengerRepository.save(passenger("EF345678", "John Smith"));

        mockMvc.perform(get("/api/passengers").param("name", "ANA").param("sort", "name,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Mariana Lima"))
                .andExpect(jsonPath("$.content[1].name").value("Ana Souza"));
    }

    @Test
    void getPassengers_returnsTheRequestedPage() throws Exception {
        for (int i = 1; i <= 5; i++) {
            passengerRepository.save(passenger("DOC0000" + i, "Passenger " + i));
        }

        mockMvc.perform(get("/api/passengers").param("page", "2").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Passenger 5"))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.last").value(true));
    }
}
