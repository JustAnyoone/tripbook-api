package com.tt.tripbook.service;

import com.tt.tripbook.dto.PageResponse;
import com.tt.tripbook.dto.PassengerRequest;
import com.tt.tripbook.dto.PassengerResponse;
import com.tt.tripbook.exception.ConflictException;
import com.tt.tripbook.exception.ResourceNotFoundException;
import com.tt.tripbook.model.AppUser;
import com.tt.tripbook.model.Passenger;
import com.tt.tripbook.repository.PassengerRepository;
import com.tt.tripbook.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PassengerService {

    private final PassengerRepository repository;
    private final CurrentUser currentUser;

    public PassengerService(PassengerRepository repository, CurrentUser currentUser) {
        this.repository = repository;
        this.currentUser = currentUser;
    }

    // Every method works on the signed-in account's passengers only.
    // Optional name filter; a null or blank value means "no filter"
    @Transactional(readOnly = true)
    public PageResponse<PassengerResponse> findAll(String name, Pageable pageable) {
        Long ownerId = currentUser.id();
        Page<Passenger> page = (name == null || name.isBlank())
                ? repository.findByOwnerId(ownerId, pageable)
                : repository.findByOwnerIdAndNameContainingIgnoreCase(ownerId, name.trim(), pageable);
        return PageResponse.from(page.map(PassengerResponse::from));
    }

    @Transactional(readOnly = true)
    public PassengerResponse findById(Long id) {
        return PassengerResponse.from(getEntity(id));
    }

    @Transactional
    public PassengerResponse create(PassengerRequest request) {
        AppUser owner = currentUser.get();
        if (repository.existsByOwnerIdAndDocumentNumber(owner.getId(), request.documentNumber())) {
            throw new ConflictException("A passenger with this document number already exists");
        }
        Passenger passenger = new Passenger();
        passenger.setOwner(owner);
        apply(request, passenger);
        return PassengerResponse.from(repository.save(passenger));
    }

    @Transactional
    public PassengerResponse update(Long id, PassengerRequest request) {
        Passenger passenger = getEntity(id);
        // Another passenger (different id) must not already own this document number
        if (repository.existsByOwnerIdAndDocumentNumberAndIdNot(currentUser.id(), request.documentNumber(), id)) {
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

    // Package-private: TripService needs the entity itself, not the DTO.
    // Another account's passenger is reported as not found, so its existence is not revealed.
    Passenger getEntity(Long id) {
        return repository.findByIdAndOwnerId(id, currentUser.id())
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