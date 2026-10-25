package com.tt.tripbook.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// Grants a guest user read access to a trip owned by another user.
@Entity
@Table(name = "trip_share",
        uniqueConstraints = @UniqueConstraint(columnNames = {"trip_id", "guest_id"}))
public class TripShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    // The account that owns the trip and created the share.
    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id")
    private AppUser owner;

    // The account that was invited to see the trip.
    @ManyToOne(optional = false)
    @JoinColumn(name = "guest_id")
    private AppUser guest;

    @Column(name = "shared_at", nullable = false)
    private LocalDateTime sharedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Trip getTrip() { return trip; }
    public void setTrip(Trip trip) { this.trip = trip; }

    public AppUser getOwner() { return owner; }
    public void setOwner(AppUser owner) { this.owner = owner; }

    public AppUser getGuest() { return guest; }
    public void setGuest(AppUser guest) { this.guest = guest; }

    public LocalDateTime getSharedAt() { return sharedAt; }
    public void setSharedAt(LocalDateTime sharedAt) { this.sharedAt = sharedAt; }
}
