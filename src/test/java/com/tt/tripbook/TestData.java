package com.tt.tripbook;

import com.tt.tripbook.model.AppUser;
import com.tt.tripbook.model.Destination;
import com.tt.tripbook.model.Passenger;
import com.tt.tripbook.model.Trip;
import com.tt.tripbook.model.TripStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

// Factory methods for valid entities, shared by the repository and integration tests.
// Every required (nullable = false) column is filled in.
public final class TestData {

    private TestData() {
    }

    // An account that can own passengers; the password hash is only a placeholder
    public static AppUser user(String email) {
        AppUser user = new AppUser();
        user.setName("Test User");
        user.setEmail(email);
        user.setPasswordHash("not-a-real-hash");
        return user;
    }

    // A passenger that belongs to the given account
    public static Passenger passenger(AppUser owner, String documentNumber) {
        Passenger passenger = passenger(documentNumber);
        passenger.setOwner(owner);
        return passenger;
    }

    public static Passenger passenger(AppUser owner, String documentNumber, String name) {
        Passenger passenger = passenger(documentNumber, name);
        passenger.setOwner(owner);
        return passenger;
    }

    public static Passenger passenger(String documentNumber) {
        return passenger(documentNumber, "Ana Souza");
    }

    public static Passenger passenger(String documentNumber, String name) {
        Passenger passenger = new Passenger();
        passenger.setName(name);
        passenger.setDocumentNumber(documentNumber);
        passenger.setEmail("ana@email.com");
        passenger.setBirthDate(LocalDate.of(1998, 5, 20));
        return passenger;
    }

    public static Destination destination(String city) {
        return destination(city, "Portugal");
    }

    public static Destination destination(String city, String country) {
        Destination destination = new Destination();
        destination.setCity(city);
        destination.setCountry(country);
        return destination;
    }

    public static Trip trip(Passenger passenger, Destination destination) {
        Trip trip = new Trip();
        trip.setDepartureDate(LocalDate.of(2026, 12, 20));
        trip.setReturnDate(LocalDate.of(2026, 12, 30));
        trip.setPrice(new BigDecimal("3500.50"));
        trip.setStatus(TripStatus.CONFIRMED);
        trip.setPassenger(passenger);
        trip.setDestination(destination);
        return trip;
    }
}
