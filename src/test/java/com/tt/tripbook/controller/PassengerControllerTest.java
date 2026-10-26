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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tt.tripbook.dto.PageResponse;
import com.tt.tripbook.dto.PassengerRequest;
import com.tt.tripbook.dto.PassengerResponse;
import com.tt.tripbook.exception.ConflictException;
import com.tt.tripbook.exception.GlobalExceptionHandler;
import com.tt.tripbook.exception.ResourceNotFoundException;
import com.tt.tripbook.service.PassengerService;
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
class PassengerControllerTest {

    // Valid body used as the base; each validation test breaks exactly one field,
    // so the error message is predictable
    private static final String VALID_PASSENGER = """
            {
              "name": "Ana Souza",
              "documentNumber": "AB123456",
              "email": "ana@email.com",
              "birthDate": "1998-05-20"
            }
            """;

    @Mock
    private PassengerService service;

    @InjectMocks
    private PassengerController controller;

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
        when(service.findAll(isNull(), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(response()), 0, 10, 1, 1, true));

        mockMvc.perform(get("/api/passengers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Ana Souza"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void findAll_passesTheFilterAndThePagingParametersToTheService() throws Exception {
        when(service.findAll(eq("ana"), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 1, 5, 0, 0, true));

        mockMvc.perform(get("/api/passengers")
                        .param("name", "ana")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "email,desc"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).findAll(eq("ana"), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
        assertThat(captor.getValue().getSort().getOrderFor("email").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void findAll_sortsByNameAscending_whenNoSortIsSent() throws Exception {
        when(service.findAll(isNull(), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 10, 0, 0, true));

        mockMvc.perform(get("/api/passengers")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).findAll(isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
        assertThat(captor.getValue().getSort().getOrderFor("name").getDirection())
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void create_returns201_withTheCreatedPassenger() throws Exception {
        when(service.create(any(PassengerRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/passengers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PASSENGER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Ana Souza"))
                .andExpect(jsonPath("$.documentNumber").value("AB123456"));
    }

    @Test
    void create_returns409_whenTheDocumentNumberIsAlreadyRegistered() throws Exception {
        when(service.create(any(PassengerRequest.class)))
                .thenThrow(new ConflictException("A passenger with this document number already exists"));

        mockMvc.perform(post("/api/passengers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PASSENGER))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("A passenger with this document number already exists"));
    }

    @Test
    void create_returns400_whenTheDocumentNumberIsTooShort() throws Exception {
        mockMvc.perform(post("/api/passengers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PASSENGER.replace("AB123456", "12")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("documentNumber: document number must have 6 to 20 letters or digits"));

        verifyNoInteractions(service);
    }

    @Test
    void create_returns400_whenTheEmailIsInvalid() throws Exception {
        mockMvc.perform(post("/api/passengers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PASSENGER.replace("ana@email.com", "not-an-email")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("email: invalid email"));

        verifyNoInteractions(service);
    }

    @Test
    void create_returns400_whenTheBirthDateIsInTheFuture() throws Exception {
        mockMvc.perform(post("/api/passengers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PASSENGER.replace("1998-05-20", "2999-01-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("birthDate: birth date must be in the past"));

        verifyNoInteractions(service);
    }

    @Test
    void findById_returns404_whenThePassengerDoesNotExist() throws Exception {
        when(service.findById(999L)).thenThrow(new ResourceNotFoundException("Passenger not found: id 999"));

        mockMvc.perform(get("/api/passengers/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Passenger not found: id 999"));
    }

    @Test
    void update_returns200_withTheUpdatedPassenger() throws Exception {
        when(service.update(any(Long.class), any(PassengerRequest.class))).thenReturn(response());

        mockMvc.perform(put("/api/passengers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PASSENGER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    private PassengerResponse response() {
        return new PassengerResponse(1L, "Ana Souza", "AB123456", "ana@email.com", LocalDate.of(1998, 5, 20), null);
    }
}
