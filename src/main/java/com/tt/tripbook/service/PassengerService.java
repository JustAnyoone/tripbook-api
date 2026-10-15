package com.tt.tripbook.service;

import com.tt.tripbook.dto.PassengerRequest;
import com.tt.tripbook.dto.PassengerResponse;
import com.tt.tripbook.exception.ConflictException;
import com.tt.tripbook.exception.ResourceNotFoundException;
import com.tt.tripbook.model.Passenger;
import com.tt.tripbook.repository.PassengerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PassengerService {

    private final PassengerRepository repository;

    public PassengerService(PassengerRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<PassengerResponse> findAll() {
        return repository.findAll().stream().map(PassengerResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PassengerResponse findById(Long id) {
        return PassengerResponse.from(getEntity(id));
    }

    @Transactional
    public PassengerResponse create(PassengerRequest request) {
        if (repository.existsByDocumentNumber(request.documentNumber())) {
            throw new ConflictException("A passenger with this document number already exists");
        }
        Passenger passenger = new Passenger();
        apply(request, passenger);
        return PassengerResponse.from(repository.save(passenger));
    }

    @Transactional
    public PassengerResponse update(Long id, PassengerRequest request) {
        Passenger passenger = getEntity(id);
        // Another passenger (different id) must not already own this document number
        if (repository.existsByDocumentNumberAndIdNot(request.documentNumber(), id)) {
            throw new ConflictException("Another passenger already has this document number");
        }
        apply(request, passenger);
        return PassengerResponse.from(repository.save(passenger));
    }

    @Transactional
    public void delete(Long id) {
        // The passenger's trips are removed too (cascade + orphanRemoval on the entity)
        repository.delete(getEntity(id));
    }

    // Package-private: TripService needs the entity itself, not the DTO
    Passenger getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger not found: id " + id));
    }

    // The trips list is never touched here, so orphanRemoval cannot delete trips by accident
    private void apply(PassengerRequest request, Passenger passenger) {
        passenger.setName(request.name());
        passenger.setDocumentNumber(request.documentNumber());
        passenger.setEmail(request.email());
        passenger.setBirthDate(request.birthDate());
    }
}