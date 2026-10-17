package com.tt.tripbook.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tt.tripbook.dto.DestinationRequest;
import com.tt.tripbook.dto.DestinationResponse;
import com.tt.tripbook.dto.PageResponse;
import com.tt.tripbook.exception.ConflictException;
import com.tt.tripbook.exception.GlobalExceptionHandler;
import com.tt.tripbook.exception.ResourceNotFoundException;
import com.tt.tripbook.service.DestinationService;
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

// Web-layer test without starting Spring Boot: MockMvc is built by hand around the controller,
// and the service is a Mockito mock. The GlobalExceptionHandler is registered so error
// responses are tested exactly as the API returns them.
@ExtendWith(MockitoExtension.class)
class DestinationControllerTest {

    @Mock
    private DestinationService service;

    @InjectMocks
    private DestinationController controller;

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
    void findAll_returns200_withThePageEnvelope() throws Exception {
        when(service.findAll(isNull(), any(Pageable.class))).thenReturn(new PageResponse<>(
                List.of(new DestinationResponse(1L, "Lisbon", "Portugal", "Capital")), 0, 10, 1, 1, true));

        mockMvc.perform(get("/api/destinations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].city").value("Lisbon"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void findAll_usesTheDefaultPaging_whenNoParametersAreSent() throws Exception {
        when(service.findAll(isNull(), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 10, 0, 0, true));

        mockMvc.perform(get("/api/destinations")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).findAll(isNull(), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
        assertThat(captor.getValue().getSort().getOrderFor("city").getDirection())
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void findAll_passesTheFilterAndThePagingParametersToTheService() throws Exception {
        when(service.findAll(eq("Portugal"), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 2, 5, 0, 0, true));

        mockMvc.perform(get("/api/destinations")
                        .param("country", "Portugal")
                        .param("page", "2")
                        .param("size", "5")
                        .param("sort", "city,desc"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).findAll(eq("Portugal"), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
        assertThat(captor.getValue().getSort().getOrderFor("city").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void findById_returns404_withTheErrorBody_whenTheDestinationDoesNotExist() throws Exception {
        when(service.findById(999L)).thenThrow(new ResourceNotFoundException("Destination not found: id 999"));

        mockMvc.perform(get("/api/destinations/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Destination not found: id 999"));
    }

    @Test
    void findById_returns400_whenTheIdIsNotANumber() throws Exception {
        mockMvc.perform(get("/api/destinations/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid value 'abc' for parameter 'id'"));

        verifyNoInteractions(service);
    }

    @Test
    void create_returns201_withTheCreatedDestination() throws Exception {
        when(service.create(any(DestinationRequest.class)))
                .thenReturn(new DestinationResponse(1L, "Lisbon", "Portugal", "Capital"));

        mockMvc.perform(post("/api/destinations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"city": "Lisbon", "country": "Portugal", "description": "Capital"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.city").value("Lisbon"));
    }

    @Test
    void create_returns400_whenTheCityIsBlank() throws Exception {
        mockMvc.perform(post("/api/destinations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"city": "", "country": "Spain"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("city: city is required"));

        verifyNoInteractions(service);
    }

    @Test
    void delete_returns204_whenTheDestinationIsRemoved() throws Exception {
        mockMvc.perform(delete("/api/destinations/1"))
                .andExpect(status().isNoContent());

        verify(service).delete(1L);
    }

    @Test
    void delete_returns409_whenTheDestinationHasTrips() throws Exception {
        doThrow(new ConflictException("Cannot delete destination: it has trips associated with it"))
                .when(service).delete(1L);

        mockMvc.perform(delete("/api/destinations/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Cannot delete destination: it has trips associated with it"));
    }
}
