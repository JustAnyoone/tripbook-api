//@Service -> regra de "nao existe, entao 404"

package com.tt.tripbook.service;

import com.tt.tripbook.exception.ConflictException;
import com.tt.tripbook.exception.ResourceNotFoundException;
import com.tt.tripbook.model.Destination;
import com.tt.tripbook.repository.DestinationRepository;
import com.tt.tripbook.repository.TripRepository;

import com.tt.tripbook.exception.ConflictException;
import com.tt.tripbook.repository.TripRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DestinationService {

    private final DestinationRepository repository;
    private final TripRepository TripRepository;

    public DestinationService(DestinationRepository repository, TripRepository TripRepository) {
        this.repository = repository;
        this.TripRepository = TripRepository;
    }

    public List<Destination> findAll() {
        return repository.findAll();
    }

    public Destination findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("destination not found: id " + id));
    }

    public Destination create(Destination Destination) {
        Destination.setId(null); // guarantees an INSERT, even if the client sends an id
        return repository.save(Destination);
    }

    public Destination update(Long id, Destination newDestination) {
        Destination existing = findById(id);
        existing.setCity(newDestination.getCity());
        existing.setCountry(newDestination.getCountry());
        existing.setDescription(newDestination.getDescription());
        return repository.save(existing);
    }

    public void delete(Long id) {
    Destination existing = findById(id);
    if (TripRepository.existsByDestinationId(id)) {
        throw new ConflictException("Cannot remove the destination: there are trips associated with it");
    }
    repository.delete(existing);
    }
}
