package com.tt.tripbook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tt.tripbook.dto.PassengerRequest;
import com.tt.tripbook.dto.PassengerResponse;
import com.tt.tripbook.exception.ConflictException;
import com.tt.tripbook.exception.ResourceNotFoundException;
import com.tt.tripbook.model.Passenger;
import com.tt.tripbook.model.Trip;
import com.tt.tripbook.repository.PassengerRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PassengerServiceTest {

    @Mock
    private PassengerRepository repository;

    @InjectMocks
    private PassengerService service;

    @Test
    void findById_throwsNotFound_whenThePassengerDoesNotExist() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void create_savesThePassenger_whenTheDocumentNumberIsFree() {
        when(repository.existsByDocumentNumber("AB123456")).thenReturn(false);
        when(repository.save(any(Passenger.class))).thenAnswer(invocation -> {
            Passenger saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        PassengerResponse response = service.create(request("Ana Souza", "AB123456"));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Ana Souza");
        assertThat(response.documentNumber()).isEqualTo("AB123456");
    }

    @Test
    void create_throwsConflict_whenTheDocumentNumberAlreadyExists() {
        when(repository.existsByDocumentNumber("AB123456")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("Ana Souza", "AB123456")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("document number");

        verify(repository, never()).save(any(Passenger.class));
    }

    @Test
    void update_changesTheFields_whenTheDocumentNumberIsTheirOwn() {
        Passenger existing = passenger(1L, "AB123456");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        // "IdNot" excludes the passenger being updated, so keeping the same document is allowed
        when(repository.existsByDocumentNumberAndIdNot("AB123456", 1L)).thenReturn(false);
        when(repository.save(any(Passenger.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PassengerResponse response = service.update(1L, request("Ana Lima", "AB123456"));

        assertThat(response.name()).isEqualTo("Ana Lima");
        assertThat(existing.getName()).isEqualTo("Ana Lima");
    }

    @Test
    void update_throwsConflict_whenTheDocumentNumberBelongsToAnotherPassenger() {
        when(repository.findById(1L)).thenReturn(Optional.of(passenger(1L, "AB123456")));
        when(repository.existsByDocumentNumberAndIdNot("CD789012", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, request("Ana Souza", "CD789012")))
                .isInstanceOf(ConflictException.class);

        verify(repository, never()).save(any(Passenger.class));
    }

    @Test
    void update_throwsNotFound_whenThePassengerDoesNotExist() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(999L, request("Ana Souza", "AB123456")))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).save(any(Passenger.class));
    }

    @Test
    void update_neverReplacesTheTripsList() {
        // The entity uses orphanRemoval = true, so replacing this list could delete trips by accident
        Passenger existing = passenger(1L, "AB123456");
        Trip trip = new Trip();
        trip.setId(5L);
        existing.getTrips().add(trip);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByDocumentNumberAndIdNot("AB123456", 1L)).thenReturn(false);
        when(repository.save(any(Passenger.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.update(1L, request("Ana Lima", "AB123456"));

        assertThat(existing.getTrips()).containsExactly(trip);
    }

    @Test
    void delete_removesThePassenger_whenItExists() {
        Passenger existing = passenger(1L, "AB123456");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        service.delete(1L);

        verify(repository).delete(existing);
    }

    @Test
    void delete_throwsNotFound_whenThePassengerDoesNotExist() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).delete(any(Passenger.class));
    }

    private PassengerRequest request(String name, String documentNumber) {
        return new PassengerRequest(name, documentNumber, "ana@email.com", LocalDate.of(1998, 5, 20));
    }

    private Passenger passenger(Long id, String documentNumber) {
        Passenger passenger = new Passenger();
        passenger.setId(id);
        passenger.setName("Ana Souza");
        passenger.setDocumentNumber(documentNumber);
        passenger.setEmail("ana@email.com");
        return passenger;
    }
}
