package com.tt.tripbook.service;

import com.tt.tripbook.dto.DestinationRequest;
import com.tt.tripbook.dto.DestinationResponse;
import com.tt.tripbook.dto.PageResponse;
import com.tt.tripbook.exception.ConflictException;
import com.tt.tripbook.exception.ResourceNotFoundException;
import com.tt.tripbook.model.Destination;
import com.tt.tripbook.repository.DestinationRepository;
import com.tt.tripbook.repository.TripRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DestinationService {

    private final DestinationRepository repository;
    private final TripRepository tripRepository;

    public DestinationService(DestinationRepository repository, TripRepository tripRepository) {
        this.repository = repository;
        this.tripRepository = tripRepository;
    }

    // Optional country filter; a null or blank value means "no filter"
    @Transactional(readOnly = true)
    public PageResponse<DestinationResponse> findAll(String country, Pageable pageable) {
        Page<Destination> page = (country == null || country.isBlank())
                ? repository.findAll(pageable)
                : repository.findByCountryContainingIgnoreCase(country.trim(), pageable);
        return PageResponse.from(page.map(DestinationResponse::from));
    }

    @Transactional(readOnly = true)
    public DestinationResponse findById(Long id) {
        return DestinationResponse.from(getEntity(id));
    }

    @Transactional
    public DestinationResponse create(DestinationRequest request) {
        String city = request.city().trim();
        String country = request.country().trim();
        if (repository.existsByCityIgnoreCaseAndCountryIgnoreCase(city, country)) {
            throw duplicate(city, country);
        }
        Destination destination = new Destination();
        apply(request, destination);
        return DestinationResponse.from(repository.save(destination));
    }

    @Transactional
    public DestinationResponse update(Long id, DestinationRequest request) {
        Destination destination = getEntity(id);
        String city = request.city().trim();
        String country = request.country().trim();
        if (repository.existsByCityIgnoreCaseAndCountryIgnoreCaseAndIdNot(city, country, id)) {
            throw duplicate(city, country);
        }
        apply(request, destination);
        return DestinationResponse.from(repository.save(destination));
    }

    @Transactional
    public void delete(Long id) {
        Destination destination = getEntity(id);
        // Business rule: a destination in use by trips cannot be removed
        if (tripRepository.existsByDestinationId(id)) {
            throw new ConflictException("Cannot delete destination: it has trips associated with it");
        }
        repository.delete(destination);
    }

    // Package-private: TripService needs the entity itself, not the DTO
    Destination getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Destination not found: id " + id));
    }

    private void apply(DestinationRequest request, Destination destination) {
        // Stored trimmed, so "Rio " and "Rio" can never become two different destinations
        destination.setCity(request.city().trim());
        destination.setCountry(request.country().trim());
        destination.setDescription(request.description());
    }

    private ConflictException duplicate(String city, String country) {
        return new ConflictException("Destination already exists: " + city + ", " + country);
    }
}
