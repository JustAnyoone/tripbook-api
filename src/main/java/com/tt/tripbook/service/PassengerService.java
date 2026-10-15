package com.tt.tripbook.service;

import com.tt.tripbook.exception.ConflictException;
import com.tt.tripbook.exception.ResourceNotFoundException;
import com.tt.tripbook.model.Passenger;
import com.tt.tripbook.repository.PassengerRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PassengerService {

        private final PassengerRepository repository;

    public PassengerService(PassengerRepository repository) {
        this.repository = repository;
    }

    public List<Passenger> findAll() {
        return repository.findAll();
    }

    public Passenger findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("passenger not found: id " + id));
    }

    public Passenger create(Passenger passenger) {
        if (repository.existsByDocumentNumber(passenger.getDocumentNumber())) {
            throw new ConflictException("A passenger with the given documentNumber already exists");
        }
        passenger.setId(null);
        // trips are created through the trips endpoint, never here
        passenger.setTrips(new ArrayList<>());
        return repository.save(passenger);
    }

    public Passenger update(Long id, Passenger newPassenger) {
        Passenger existing = findById(id);
        if (repository.existsByDocumentNumberAndIdNot(newPassenger.getDocumentNumber(), id)) {
            throw new ConflictException("Another passenger with the given documentNumber already exists");
        }

        existing.setDocumentNumber(newPassenger.getDocumentNumber());
        existing.setEmail(newPassenger.getEmail());
        existing.setBirthDate(newPassenger.getBirthDate());
        // The trips list is intentionally not copied: with orphanRemoval = true,
        // a list coming from the client could delete existing trips.
        return repository.save(existing);
    }

    public void delete(Long id) {
        // The passenger's trips are removed along with it (cascade + orphanRemoval)
        repository.delete(findById(id));
    }
}
