package com.tt.tripbook.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tt.tripbook.dto.DestinationResponse;
import com.tt.tripbook.dto.PageResponse;
import com.tt.tripbook.dto.PassengerResponse;
import com.tt.tripbook.dto.TripRequest;
import com.tt.tripbook.dto.TripResponse;
import com.tt.tripbook.exception.BusinessRuleException;
import com.tt.tripbook.exception.GlobalExceptionHandler;
import com.tt.tripbook.exception.ResourceNotFoundException;
import com.tt.tripbook.service.TripService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class TripControllerTest {

    // Valid body used as the base; each validation test breaks exactly one field
    private static final String VALID_TRIP = """
            {
              "departureDate": "2026-12-20",
              "returnDate": "2026-12-30",
              "price": 3500.50,
              "status": "CONFIRMED",
              "passengerId": 1,
              "destinationId": 1
            }
            """;

    @Mock
    private TripService service;

    @InjectMocks
    private TripController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // The Pageable resolver is registered by hand: standalone MockMvc has no Spring Boot auto-configuration
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void create_returns201_withThePassengerAndDestinationInsideTheTrip() throws Exception {
        when(service.create(any(TripRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_TRIP))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.passenger.name").value("Ana Souza"))
                .andExpect(jsonPath("$.destination.city").value("Lisbon"));
    }

    @Test
    void create_returns400_whenTheStatusIsNotOneOfTheAllowedValues() throws Exception {
        mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_TRIP.replace("\"CONFIRMED\"", "\"XYZ\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("status: status must be PENDING, CONFIRMED or CANCELLED"));

        verifyNoInteractions(service);
    }

    @Test
    void create_returns400_whenTheCurrencyIsNotSupported() throws Exception {
        mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_TRIP.replace("\"price\": 3500.50,", "\"price\": 3500.50, \"currency\": \"JPY\",")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("currency: currency must be BRL, USD or EUR"));

        verifyNoInteractions(service);
    }

    @Test
    void create_returns400_whenThePriceIsNegative() throws Exception {
        mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_TRIP.replace("3500.50", "-10")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("price: price must be greater than zero"));

        verifyNoInteractions(service);
    }

    @Test
    void create_returns400_whenThePassengerIdIsMissing() throws Exception {
        // Removing the whole line keeps the JSON valid because it is not the last property
        mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_TRIP.replace("\"passengerId\": 1,", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("passengerId: passengerId is required"));

        verifyNoInteractions(service);
    }

    @Test
    void create_returns400_whenTheServiceRejectsTheDates() throws Exception {
        when(service.create(any(TripRequest.class)))
                .thenThrow(new BusinessRuleException("Return date cannot be before departure date"));

        mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_TRIP.replace("2026-12-30", "2026-12-10")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Return date cannot be before departure date"));
    }

    @Test
    void create_returns404_whenThePassengerDoesNotExist() throws Exception {
        when(service.create(any(TripRequest.class)))
                .thenThrow(new ResourceNotFoundException("Passenger not found: id 999"));

        mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_TRIP.replace("\"passengerId\": 1", "\"passengerId\": 999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Passenger not found: id 999"));
    }

    @Test
    void create_returns400_whenTheJsonIsMalformed() throws Exception {
        mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"price\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(service);
    }

    @Test
    void create_returns400_whenADateIsInvalid() throws Exception {
        mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_TRIP.replace("2026-12-20", "2026-13-45")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(service);
    }

    @Test
    void create_returns400_whenTheDateDoesNotExistInTheCalendar() throws Exception {
        // 31 November, 30 February and 29 February of a non-leap year are all impossible dates
        for (String impossible : new String[] {"2026-11-31", "2026-02-30", "2027-02-29"}) {
            mockMvc.perform(post("/api/trips")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_TRIP.replace("2026-12-20", impossible)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            "Malformed JSON or invalid value in the request body (dates must be real calendar dates in yyyy-MM-dd format)"));
        }

        verifyNoInteractions(service);
    }

    @Test
    void findAll_returns200_withThePageEnvelope() throws Exception {
        when(service.findAll(isNull(), any(Pageable.class))).thenReturn(pageOf(response()));

        mockMvc.perform(get("/api/trips"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void findAll_showsTheNewestDepartureFirst_whenNoSortIsSent() throws Exception {
        when(service.findAll(isNull(), any(Pageable.class))).thenReturn(pageOf());

        mockMvc.perform(get("/api/trips")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).findAll(isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
        assertThat(captor.getValue().getSort().getOrderFor("departureDate").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void findAll_passesTheStatusAndThePagingParametersToTheService() throws Exception {
        when(service.findAll(eq("CONFIRMED"), any(Pageable.class))).thenReturn(pageOf());

        mockMvc.perform(get("/api/trips")
                        .param("status", "CONFIRMED")
                        .param("page", "3")
                        .param("size", "20"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).findAll(eq("CONFIRMED"), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(3);
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void findAll_returns400_whenTheServiceRejectsTheStatus() throws Exception {
        when(service.findAll(eq("XYZ"), any(Pageable.class)))
                .thenThrow(new BusinessRuleException("status must be PENDING, CONFIRMED or CANCELLED"));

        mockMvc.perform(get("/api/trips").param("status", "XYZ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("status must be PENDING, CONFIRMED or CANCELLED"));
    }

    @Test
    void byPassenger_returns200_withThatPassengersTrips() throws Exception {
        when(service.findByPassenger(eq(1L), any(Pageable.class))).thenReturn(pageOf(response()));

        mockMvc.perform(get("/api/trips/passenger/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].passenger.id").value(1));
    }

    @Test
    void byPassenger_returns404_whenThePassengerDoesNotExist() throws Exception {
        when(service.findByPassenger(eq(999L), any(Pageable.class)))
                .thenThrow(new ResourceNotFoundException("Passenger not found: id 999"));

        mockMvc.perform(get("/api/trips/passenger/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void byDestination_returns200_withTheTripsToThatDestination() throws Exception {
        when(service.findByDestination(eq(1L), any(Pageable.class))).thenReturn(pageOf(response()));

        mockMvc.perform(get("/api/trips/destination/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].destination.city").value("Lisbon"));
    }

    private PageResponse<TripResponse> pageOf(TripResponse... items) {
        return new PageResponse<>(List.of(items), 0, 10, items.length, 1, true);
    }

    private TripResponse response() {
        return new TripResponse(
                1L,
                LocalDate.of(2026, 12, 20),
                LocalDate.of(2026, 12, 30),
                new BigDecimal("3500.50"),
                "BRL",
                "CONFIRMED",
                new PassengerResponse(1L, "Ana Souza", "AB123456", "ana@email.com", LocalDate.of(1998, 5, 20)),
                new DestinationResponse(1L, "Lisbon", "Portugal", "Portuguese capital"));
    }
}
