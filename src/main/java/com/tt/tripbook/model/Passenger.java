package com.tt.tripbook.model;


import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Entity
// A document number is unique per account, so two users can each register the same person
@Table(name = "passenger", uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "document_number"}))
public class Passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) //Database constraint
    private String name;


    @Column(name = "document_number", nullable = false, length = 20)
    private String documentNumber;


    @Column(nullable = false)
    private String email;


    @Column(name = "birth_date")
    private LocalDate birthDate;

    // Cloudinary URL for the passenger's profile photo. Null means no photo uploaded yet.
    @Column(name = "photo_url")
    private String photoUrl;

    // The account this passenger belongs to. Nullable in the schema only so that rows created before
    // accounts owned data can be adopted by LegacyDataMigration; the service always sets it.
    @ManyToOne
    @JoinColumn(name = "owner_id")
    private AppUser owner;

    @OneToMany(mappedBy = "passenger", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Trip> trips = new ArrayList<>();

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDocumentNumber() {
        return documentNumber;
    }
    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public LocalDate getBirthDate() {
        return birthDate;
    }
    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }
    public AppUser getOwner() {
        return owner;
    }
    public void setOwner(AppUser owner) {
        this.owner = owner;
    }
    public String getPhotoUrl() {
        return photoUrl;
    }
    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
    public List<Trip> getTrips() {
        return trips;
    }
    public void setTrips(List<Trip> trips) {
        this.trips = trips;
    }

    

    
}
