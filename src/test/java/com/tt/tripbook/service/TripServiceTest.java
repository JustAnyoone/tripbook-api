package com.tt.tripbook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tt.tripbook.dto.PageResponse;
import com.tt.tripbook.dto.TripRequest;
import com.tt.tripbook.dto.TripResponse;
import com.tt.tripbook.exception.BusinessRuleException;
import com.tt.tripbook.exception.ResourceNotFoundException;
import com.tt.tripbook.model.Destination;
import com.tt.tripbook.model.Passenger;
import com.tt.tripbook.model.Trip;
import com.tt.tripbook.repository.TripRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

// The business rules live here, so this is the most important service test.
// This class sits in the same package as the services because getEntity(...) is package-private.
@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private TripRepository repository;

    @Mock
    private PassengerService passengerService;

    @Mock
    private DestinationService destinationService;

    @InjectMocks
    private TripService service;

    @Test
    void create_savesTheTrip_withTheLoadedPassengerAndDestination() {
        when(passengerService.getEntity(1L)).thenReturn(passenger(1L));
        when(destinationService.getEntity(1L)).thenReturn(destination(1L));
        when(repository.save(any(Trip.class))).thenAnswer(invocation -> {
            Trip saved = invocation.getArgument(0);
            saved.setId(7L);
            return saved;
        });

        TripResponse response = service.create(request("CONFIRMED", LocalDate.of(2026, 12, 30)));

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.status()).isEqualTo("CONFIRMED");
        assertThat(response.price()).isEqualByComparingTo("3500.50");
        assertThat(response.passenger().name()).isEqualTo("Ana Souza");
        assertThat(response.destination().city()).isEqualTo("Lisbon");
    }

    @Test
    void create_acceptsATripWithoutReturnDate() {
        when(passengerService.getEntity(1L)).thenReturn(passenger(1L));
        when(destinationService.getEntity(1L)).thenReturn(destination(1L));
        when(repository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TripResponse response = service.create(request("PENDING", null));

        assertThat(response.returnDate()).isNull();
    }

    @Test
    void create_throwsBusinessRule_whenTheReturnDateIsBeforeTheDepartureDate() {
        TripRequest invalid = request("CONFIRMED", LocalDate.of(2026, 12, 10));

        assertThatThrownBy(() -> service.create(invalid))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Return date");

        // The rule is checked first, so nothing is loaded or saved
        verifyNoInteractions(repository, passengerService, destinationService);
    }

    @Test
    void create_throwsNotFound_whenThePassengerDoesNotExist() {
        when(passengerService.getEntity(1L)).thenThrow(new ResourceNotFoundException("Passenger not found: id 1"));

        assertThatThrownBy(() -> service.create(request("CONFIRMED", LocalDate.of(2026, 12, 30))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Passenger");

        verify(repository, never()).save(any(Trip.class));
    }

    @Test
    void create_throwsNotFound_whenTheDestinationDoesNotExist() {
        when(passengerService.getEntity(1L)).thenReturn(passenger(1L));
        when(destinationService.getEntity(1L)).thenThrow(new ResourceNotFoundException("Destination not found: id 1"));

        assertThatThrownBy(() -> service.create(request("CONFIRMED", LocalDate.of(2026, 12, 30))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Destination");

        verify(repository, never()).save(any(Trip.class));
    }

    @Test
    void update_changesTheStatus_ofAnExistingTrip() {
        Trip existing = trip(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(passengerService.getEntity(1L)).thenReturn(passenger(1L));
        when(destinationService.getEntity(1L)).thenReturn(destination(1L));
        when(repository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TripResponse response = service.update(1L, request("CANCELLED", LocalDate.of(2026, 12, 30)));

        assertThat(response.status()).isEqualTo("CANCELLED");
        assertThat(existing.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void update_throwsNotFound_whenTheTripDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, request("CONFIRMED", LocalDate.of(2026, 12, 30))))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(passengerService, destinationService);
    }

    @Test
    void findById_throwsNotFound_whenTheTripDoesNotExist() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void findAll_withoutStatus_returnsThePageOfEveryTrip() {
        Pageable pageable = PageRequest.of(0, 10);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(trip(10L), trip(11L)), pageable, 2));

        PageResponse<TripResponse> result = service.findAll(null, pageable);

        assertThat(result.content()).extracting(TripResponse::id).containsExactly(10L, 11L);
        assertThat(result.totalElements()).isEqualTo(2);
        verify(repository, never()).findByStatus(any(), any());
    }

    @Test
    void findAll_withStatus_filtersByTheNormalizedStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        when(repository.findByStatus("CONFIRMED", pageable))
                .thenReturn(new PageImpl<>(List.of(trip(10L)), pageable, 1));

        // Lower case and extra spaces are accepted and normalized
        PageResponse<TripResponse> result = service.findAll("  confirmed ", pageable);

        assertThat(result.content()).extracting(TripResponse::id).containsExactly(10L);
        verify(repository, never()).findAll(any(Pageable.class));
    }

    @Test
    void findAll_throwsBusinessRule_whenTheStatusIsUnknown() {
        assertThatThrownBy(() -> service.findAll("XYZ", PageRequest.of(0, 10)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PENDING, CONFIRMED or CANCELLED");

        verifyNoInteractions(repository);
    }

    @Test
    void findByPassenger_returnsTheTripsOfThatPassenger() {
        Pageable pageable = PageRequest.of(0, 10);
        when(passengerService.getEntity(1L)).thenReturn(passenger(1L));
        when(repository.findByPassengerId(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(trip(10L), trip(11L)), pageable, 2));

        PageResponse<TripResponse> result = service.findByPassenger(1L, pageable);

        assertThat(result.content()).extracting(TripResponse::id).containsExactly(10L, 11L);
    }

    @Test
    void findByPassenger_throwsNotFound_whenThePassengerDoesNotExist() {
        when(passengerService.getEntity(5L)).thenThrow(new ResourceNotFoundException("Passenger not found: id 5"));

        assertThatThrownBy(() -> service.findByPassenger(5L, PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).findByPassengerId(anyLong(), any(Pageable.class));
    }

    @Test
    void findByDestination_returnsTheTripsToThatDestination() {
        Pageable pageable = PageRequest.of(0, 10);
        when(destinationService.getEntity(1L)).thenReturn(destination(1L));
        when(repository.findByDestinationId(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(trip(10L)), pageable, 1));

        PageResponse<TripResponse> result = service.findByDestination(1L, pageable);

        assertThat(result.content()).extracting(TripResponse::id).containsExactly(10L);
    }

    @Test
    void findByDestination_throwsNotFound_whenTheDestinationDoesNotExist() {
        when(destinationService.getEntity(9L)).thenThrow(new ResourceNotFoundException("Destination not found: id 9"));

        assertThatThrownBy(() -> service.findByDestination(9L, PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).findByDestinationId(anyLong(), any(Pageable.class));
    }

    @Test
    void delete_removesTheTrip_whenItExists() {
        Trip existing = trip(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        service.delete(1L);

        verify(repository).delete(existing);
    }

    private TripRequest request(String status, LocalDate returnDate) {
        return new TripRequest(LocalDate.of(2026, 12, 20), returnDate, new BigDecimal("3500.50"), status, 1L, 1L);
    }

    private Passenger passenger(Long id) {
        Passenger passenger = new Passenger();
        passenger.setId(id);
        passenger.setName("Ana Souza");
        passenger.setDocumentNumber("AB123456");
        passenger.setEmail("ana@email.com");
        return passenger;
    }

    private Destination destination(Long id) {
        Destination destination = new Destination();
        destination.setId(id);
        destination.setCity("Lisbon");
        destination.setCountry("Portugal");
        return destination;
    }

    // A fully populated trip: TripResponse.from(...) reads the passenger and the destination
    private Trip trip(Long id) {
        Trip trip = new Trip();
        trip.setId(id);
        trip.setDepartureDate(LocalDate.of(2026, 12, 20));
        trip.setPrice(new BigDecimal("1000.00"));
        trip.setStatus("PENDING");
        trip.setPassenger(passenger(1L));
        trip.setDestination(destination(1L));
        return trip;
    }
}
