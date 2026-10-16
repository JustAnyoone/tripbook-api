package com.tt.tripbook;

import com.tt.tripbook.model.Destination;
import com.tt.tripbook.model.Passenger;
import com.tt.tripbook.model.Trip;
import java.math.BigDecimal;
import java.time.LocalDate;

// Factory methods for valid entities, shared by the repository and integration tests.
// Every required (nullable = false) column is filled in.
public final class TestData {

    private TestData() {
    }

    public static Passenger passenger(String documentNumber) {
        Passenger passenger = new Passenger();
        passenger.setName("Ana Souza");
        passenger.setDocumentNumber(documentNumber);
        passenger.setEmail("ana@email.com");
        passenger.setBirthDate(LocalDate.of(1998, 5, 20));
        return passenger;
    }

    public static Destination destination(String city) {
        Destination destination = new Destination();
        destination.setCity(city);
        destination.setCountry("Portugal");
        return destination;
    }

    public static Trip trip(Passenger passenger, Destination destination) {
        Trip trip = new Trip();
        trip.setDepartureDate(LocalDate.of(2026, 12, 20));
        trip.setReturnDate(LocalDate.of(2026, 12, 30));
        trip.setPrice(new BigDecimal("3500.50"));
        trip.setStatus("CONFIRMED");
        trip.setPassenger(passenger);
        trip.setDestination(destination);
        return trip;
    }
}
