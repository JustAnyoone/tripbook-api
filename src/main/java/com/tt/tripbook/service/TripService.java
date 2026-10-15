package com.tt.tripbook.service;

import com.tt.tripbook.dto.TripRequest;
import com.tt.tripbook.dto.TripResponse;
import com.tt.tripbook.exception.BusinessRuleException;
import com.tt.tripbook.exception.ResourceNotFoundException;
import com.tt.tripbook.model.Trip;
import com.tt.tripbook.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TripService {

    private final TripRepository repository;
    private final PassengerService passengerService;
    private final DestinationService destinationService;

    public TripService(TripRepository repository,
                       PassengerService passengerService,
                       DestinationService destinationService) {
        this.repository = repository;
        this.passengerService = passengerService;
        this.destinationService = destinationService;
    }

    @Transactional(readOnly = true)
    public List<TripResponse> findAll() {
        return repository.findAll().stream().map(TripResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TripResponse findById(Long id) {
        return TripResponse.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<TripResponse> findByPassenger(Long passengerId) {
        passengerService.getEntity(passengerId); // 404 if the passenger does not exist
        return repository.findByPassengerId(passengerId).stream().map(TripResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TripResponse> findByDestination(Long destinationId) {
        destinationService.getEntity(destinationId); // 404 if the destination does not exist
        return repository.findByDestinationId(destinationId).stream().map(TripResponse::from).toList();
    }

    @Transactional
    public TripResponse create(TripRequest request) {
        validateDates(request);
        Trip trip = new Trip();
        apply(request, trip);
        return TripResponse.from(repository.save(trip));
    }

    @Transactional
    public TripResponse update(Long id, TripRequest request) {
        Trip trip = getEntity(id);
        validateDates(request);
        apply(request, trip);
        return TripResponse.from(repository.save(trip));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getEntity(id));
    }

    private Trip getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: id " + id));
    }

    // Rule: the return date cannot be before the departure date
    private void validateDates(TripRequest request) {
        if (request.returnDate() != null && request.returnDate().isBefore(request.departureDate())) {
            throw new BusinessRuleException("Return date cannot be before departure date");
        }
    }

    // The request carries only ids; the passenger and destination must exist, so we load the real entities
    private void apply(TripRequest request, Trip trip) {
        trip.setDepartureDate(request.departureDate());
        trip.setReturnDate(request.returnDate());
        trip.setPrice(request.price());
        trip.setStatus(request.status());
        trip.setPassenger(passengerService.getEntity(request.passengerId()));
        trip.setDestination(destinationService.getEntity(request.destinationId()));
    }
}