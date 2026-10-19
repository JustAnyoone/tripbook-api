package com.tt.tripbook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tt.tripbook.dto.DestinationRequest;
import com.tt.tripbook.dto.DestinationResponse;
import com.tt.tripbook.dto.PageResponse;
import com.tt.tripbook.exception.ConflictException;
import com.tt.tripbook.exception.ResourceNotFoundException;
import com.tt.tripbook.model.Destination;
import com.tt.tripbook.repository.DestinationRepository;
import com.tt.tripbook.repository.TripRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

// Pure unit test: no Spring context and no database. The repositories are Mockito mocks.
@ExtendWith(MockitoExtension.class)
class DestinationServiceTest {

    @Mock
    private DestinationRepository repository;

    @Mock
    private TripRepository tripRepository;

    @InjectMocks
    private DestinationService service;

    @Test
    void findAll_withoutCountry_returnsThePageOfEveryDestination() {
        Pageable pageable = PageRequest.of(0, 10);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(
                destination(1L, "Lisbon", "Portugal"),
                destination(2L, "Tokyo", "Japan")), pageable, 2));

        PageResponse<DestinationResponse> result = service.findAll(null, pageable);

        assertThat(result.content()).extracting(DestinationResponse::city).containsExactly("Lisbon", "Tokyo");
        assertThat(result.totalElements()).isEqualTo(2);
        verify(repository, never()).findByCountryContainingIgnoreCase(any(), any());
    }

    @Test
    void findAll_withBlankCountry_ignoresTheFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.findAll("   ", pageable);

        verify(repository).findAll(pageable);
        verify(repository, never()).findByCountryContainingIgnoreCase(any(), any());
    }

    @Test
    void findAll_withCountry_filtersByTheTrimmedCountry() {
        Pageable pageable = PageRequest.of(1, 5);
        when(repository.findByCountryContainingIgnoreCase("Portugal", pageable))
                .thenReturn(new PageImpl<>(List.of(destination(1L, "Lisbon", "Portugal")), pageable, 6));

        PageResponse<DestinationResponse> result = service.findAll("  Portugal ", pageable);

        assertThat(result.content()).extracting(DestinationResponse::city).containsExactly("Lisbon");
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(5);
        verify(repository, never()).findAll(any(Pageable.class));
    }

    @Test
    void findById_returnsTheDestination_whenItExists() {
        when(repository.findById(1L)).thenReturn(Optional.of(destination(1L, "Lisbon", "Portugal")));

        DestinationResponse response = service.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.city()).isEqualTo("Lisbon");
        assertThat(response.country()).isEqualTo("Portugal");
    }

    @Test
    void findById_throwsNotFound_whenItDoesNotExist() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void create_copiesTheRequestFieldsAndReturnsTheGeneratedId() {
        // Simulates the database assigning an id on save
        when(repository.save(any(Destination.class))).thenAnswer(invocation -> {
            Destination saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        DestinationResponse response = service.create(new DestinationRequest("Tokyo", "Japan", "Japanese capital"));

        ArgumentCaptor<Destination> captor = ArgumentCaptor.forClass(Destination.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCity()).isEqualTo("Tokyo");
        assertThat(captor.getValue().getCountry()).isEqualTo("Japan");
        assertThat(captor.getValue().getDescription()).isEqualTo("Japanese capital");
        assertThat(response.id()).isEqualTo(10L);
    }

    @Test
    void update_overwritesTheFieldsOfTheExistingDestination() {
        Destination existing = destination(1L, "Lisbon", "Portugal");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Destination.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DestinationResponse response = service.update(1L, new DestinationRequest("Porto", "Portugal", "Northern city"));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.city()).isEqualTo("Porto");
        assertThat(existing.getDescription()).isEqualTo("Northern city");
    }

    @Test
    void create_throwsConflict_whenTheCityAndCountryAlreadyExist() {
        when(repository.existsByCityIgnoreCaseAndCountryIgnoreCase("Rio de Janeiro", "Brazil")).thenReturn(true);

        // Extra spaces must not hide the duplicate
        assertThatThrownBy(() -> service.create(new DestinationRequest("  Rio de Janeiro ", "Brazil ", "again")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Rio de Janeiro");

        verify(repository, never()).save(any(Destination.class));
    }

    @Test
    void create_storesTheCityAndCountryTrimmed() {
        when(repository.save(any(Destination.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DestinationResponse response = service.create(new DestinationRequest("  Tokyo ", " Japan  ", null));

        assertThat(response.city()).isEqualTo("Tokyo");
        assertThat(response.country()).isEqualTo("Japan");
    }

    @Test
    void update_throwsConflict_whenAnotherDestinationHasTheSameCityAndCountry() {
        when(repository.findById(1L)).thenReturn(Optional.of(destination(1L, "Lisbon", "Portugal")));
        when(repository.existsByCityIgnoreCaseAndCountryIgnoreCaseAndIdNot("Porto", "Portugal", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, new DestinationRequest("Porto", "Portugal", null)))
                .isInstanceOf(ConflictException.class);

        verify(repository, never()).save(any(Destination.class));
    }

    @Test
    void update_throwsNotFound_whenTheDestinationDoesNotExist() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(999L, new DestinationRequest("X", "Y", null)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).save(any(Destination.class));
    }

    @Test
    void delete_removesTheDestination_whenItHasNoTrips() {
        Destination existing = destination(1L, "Lisbon", "Portugal");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(tripRepository.existsByDestinationId(1L)).thenReturn(false);

        service.delete(1L);

        verify(repository).delete(existing);
    }

    @Test
    void delete_throwsConflict_whenTheDestinationHasTrips() {
        when(repository.findById(1L)).thenReturn(Optional.of(destination(1L, "Lisbon", "Portugal")));
        when(tripRepository.existsByDestinationId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("trips");

        verify(repository, never()).delete(any(Destination.class));
    }

    @Test
    void delete_throwsNotFound_beforeLookingAtTrips_whenTheDestinationDoesNotExist() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(tripRepository);
    }

    private Destination destination(Long id, String city, String country) {
        Destination destination = new Destination();
        destination.setId(id);
        destination.setCity(city);
        destination.setCountry(country);
        return destination;
    }
}
