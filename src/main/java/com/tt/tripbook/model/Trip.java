package com.tt.tripbook.model;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "trip")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "departure_date", nullable = false)
    private LocalDate departureDate;


    @Column(name = "return_date")
    private LocalDate returnDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;


    // The default also fills the rows that existed before this column was added (ddl-auto=update)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    @ColumnDefault("'BRL'")
    private Currency currency = Currency.BRL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TripStatus status;


    @ManyToOne(optional = false)
    @JoinColumn(name = "passenger_id")
    private Passenger passenger;


    @ManyToOne(optional = false)
    @JoinColumn(name = "destination_id")
    private Destination destination;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(LocalDate departureDate) {
        this.departureDate = departureDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public TripStatus getStatus() {
        return status;
    }

    public void setStatus(TripStatus status) {
        this.status = status;
    }

    public Passenger getPassenger() {
        return passenger;
    }

    public void setPassenger(Passenger passenger) {
        this.passenger = passenger;
    }

    public Destination getDestination() {
        return destination;
    }

    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    
}
