package com.tt.tripbook.service;

import com.tt.tripbook.dto.PageResponse;
import com.tt.tripbook.dto.TripRequest;
import com.tt.tripbook.dto.TripResponse;
import com.tt.tripbook.exception.BusinessRuleException;
import com.tt.tripbook.exception.ResourceNotFoundException;
import com.tt.tripbook.model.Trip;
import com.tt.tripbook.model.TripStatus;
import com.tt.tripbook.repository.TripRepository;
import com.tt.tripbook.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;

@Service
public class TripService {

    private final TripRepository repository;
    private final PassengerService passengerService;
    private final DestinationService destinationService;
    private final Clock clock;
    private final CurrentUser currentUser;

    public TripService(TripRepository repository,
                       PassengerService passengerService,
                       DestinationService destinationService,
                       Clock clock,
                       CurrentUser currentUser) {
        this.repository = repository;
        this.passengerService = passengerService;
        this.destinationService = destinationService;
        this.clock = clock;
        this.currentUser = currentUser;
    }

    // Optional status filter (case-insensitive); a null or blank value means "no filter"
    @Transactional(readOnly = true)
    public PageResponse<TripResponse> findAll(String status, Pageable pageable) {
        // Only the trips of the signed-in account's passengers
        Long ownerId = currentUser.id();
        Page<Trip> page = (status == null || status.isBlank())
                ? repository.findByPassengerOwnerId(ownerId, pageable)
                : repository.findByPassengerOwnerIdAndStatus(ownerId, normalizeStatus(status), pageable);
        return PageResponse.from(page.map(TripResponse::from));
    }

    @Transactional(readOnly = true)
    public TripResponse findById(Long id) {
        return TripResponse.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<TripResponse> findByPassenger(Long passengerId, Pageable pageable) {
        passengerService.getEntity(passengerId); // 404 if the passenger does not exist or is not yours
        return PageResponse.from(repository.findByPassengerIdAndPassengerOwnerId(passengerId, currentUser.id(), pageable)
                .map(TripResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<TripResponse> findByDestination(Long destinationId, Pageable pageable) {
        destinationService.getEntity(destinationId); // 404 if the destination does not exist
        // Destinations are shared, but the list only holds the trips of this account
        return PageResponse.from(repository.findByDestinationIdAndPassengerOwnerId(destinationId, currentUser.id(), pageable)
                .map(TripResponse::from));
    }

    @Transactional
    public TripResponse create(TripRequest request) {
        validateDates(request, null);
        Trip trip = new Trip();
        apply(request, trip);
        return TripResponse.from(repository.save(trip));
    }

    @Transactional
    public TripResponse update(Long id, TripRequest request) {
        Trip trip = getEntity(id);
        validateDates(request, trip);
        apply(request, trip);
        return TripResponse.from(repository.save(trip));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getEntity(id));
    }

    private Trip getEntity(Long id) {
        return repository.findByIdAndPassengerOwnerId(id, currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: id " + id));
    }

    // An unknown status in the filter is a client error, not an empty result
    private TripStatus normalizeStatus(String status) {
        try {
            return TripStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("status must be PENDING, CONFIRMED or CANCELLED");
        }
    }

    // Rules: a trip cannot be scheduled in the past, and the return date cannot be before the departure date.
    // When editing, a date that was not changed is accepted even if it has passed since, so the status
    // or the price of a trip that already happened can still be corrected.
    private void validateDates(TripRequest request, Trip existing) {
        LocalDate today = LocalDate.now(clock);
        if (request.departureDate().isBefore(today)
                && (existing == null || !request.departureDate().equals(existing.getDepartureDate()))) {
            throw new BusinessRuleException("Departure date cannot be in the past");
        }
        if (request.returnDate() != null && request.returnDate().isBefore(today)
                && (existing == null || !request.returnDate().equals(existing.getReturnDate()))) {
            throw new BusinessRuleException("Return date cannot be in the past");
        }
        if (request.returnDate() != null && request.returnDate().isBefore(request.departureDate())) {
            throw new BusinessRuleException("Return date cannot be before departure date");
        }
    }

    // The request carries only ids; the passenger and destination must exist, so we load the real entities
    private void apply(TripRequest request, Trip trip) {
        trip.setDepartureDate(request.departureDate());
        trip.setReturnDate(request.returnDate());
        trip.setPrice(request.price());
        // The request validation already guarantees one of the enum names
        trip.setStatus(TripStatus.valueOf(request.status()));
        trip.setPassenger(passengerService.getEntity(request.passengerId()));
        trip.setDestination(destinationService.getEntity(request.destinationId()));
    }
}