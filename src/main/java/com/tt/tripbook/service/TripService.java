package com.tt.tripbook.service;

import com.tt.tripbook.exception.ResourceNotFoundException;
import com.tt.tripbook.exception.BusinessRuleException;
import com.tt.tripbook.model.Destination;
import com.tt.tripbook.model.Passenger;
import com.tt.tripbook.model.Trip;
import com.tt.tripbook.repository.TripRepository;
import org.springframework.stereotype.Service;

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

    public List<Trip> findAll() {
        return repository.findAll();
    }

    public Trip findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: id " + id));
    }

    public List<Trip> findAllPassenger(Long passengerId) {
        passengerService.findById(passengerId); // 404 if the passenger does not exist
        return repository.findByPassengerId(passengerId);
    }

    public List<Trip> findAllDestination(Long destinationId) {
        destinationService.findById(destinationId); // 404 if the destination does not exist
        return repository.findByDestinationId(destinationId);
    }

    public Trip create(Trip Trip) {
        validateDates(Trip);
        Trip.setId(null);
        Trip.setPassenger(resolvePassenger(Trip));
        Trip.setDestination(resolveDestination(Trip));
        return repository.save(Trip);
    }

    public Trip update(Long id, Trip newTrip) {
        Trip existing = findById(id);
        validateDates(newTrip);
        existing.setDepartureDate(newTrip.getDepartureDate());
        existing.setReturnDate(newTrip.getReturnDate());
        existing.setPrice(newTrip.getPrice());
        existing.setStatus(newTrip.getStatus());
        existing.setPassenger(resolvePassenger(newTrip));
        existing.setDestination(resolveDestination(newTrip));
        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.delete(findById(id));
    }

    // Rule: the return date cannot be before the departure date
    private void validateDates(Trip Trip) {
        if (Trip.getReturnDate() != null && Trip.getReturnDate().isBefore(Trip.getDepartureDate())) {
            throw new BusinessRuleException("The return date cannot be before the departure date");
        }
    }

    // Rule: the given passenger must exist. The JSON carries only the id,
    // so we fetch the full object from the database.
    private Passenger resolvePassenger(Trip Trip) {
        if (Trip.getPassenger().getId() == null) {
            throw new BusinessRuleException("Provide the passenger id");
        }
        return passengerService.findById(Trip.getPassenger().getId());
    }

    private Destination resolveDestination(Trip Trip) {
        if (Trip.getDestination().getId() == null) {
            throw new BusinessRuleException("Provide the destination id");
        }
        return destinationService.findById(Trip.getDestination().getId());
    }
}
