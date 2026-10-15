package com.tt.tripbook.service;

import com.tt.tripbook.dto.DestinationRequest;
import com.tt.tripbook.dto.DestinationResponse;
import com.tt.tripbook.exception.ConflictException;
import com.tt.tripbook.exception.ResourceNotFoundException;
import com.tt.tripbook.model.Destination;
import com.tt.tripbook.repository.DestinationRepository;
import com.tt.tripbook.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DestinationService {

    private final DestinationRepository repository;
    private final TripRepository tripRepository;

    public DestinationService(DestinationRepository repository, TripRepository tripRepository) {
        this.repository = repository;
        this.tripRepository = tripRepository;
    }

    @Transactional(readOnly = true)
    public List<DestinationResponse> findAll() {
        return repository.findAll().stream().map(DestinationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DestinationResponse findById(Long id) {
        return DestinationResponse.from(getEntity(id));
    }

    @Transactional
    public DestinationResponse create(DestinationRequest request) {
        Destination destination = new Destination();
        apply(request, destination);
        return DestinationResponse.from(repository.save(destination));
    }

    @Transactional
    public DestinationResponse update(Long id, DestinationRequest request) {
        Destination destination = getEntity(id);
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
        destination.setCity(request.city());
        destination.setCountry(request.country());
        destination.setDescription(request.description());
    }
}