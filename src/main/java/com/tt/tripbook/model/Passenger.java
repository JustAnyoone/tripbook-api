package com.tt.tripbook.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Pessoa que realiza trips")
@Entity
@Table(name = "passenger")
public class Passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Schema(description = "full name", example = "Ana Souza")
    @NotBlank(message = "the name is required") //It's a validation, which rejects the data before it reaches the database and allows a custom message
    @Column(nullable = false) //Database constraint
    private String name;

    @Schema(description = "Identity document number, 6 to 20 letters or digits, without dots or dashes", example = "12345678901")
    @NotBlank(message = "the documentNumber is required")
    @Pattern(regexp = "[A-Za-z0-9]{6,20}", message = "document number must have 6 to 20 letters or digits")
    @Column(nullable = false, unique = true, length = 20)
    private String documentNumber;

    @Schema(description = "Contact e-mail", example = "ana@email.com")
    @NotBlank(message = "the e-mail is required")
    @Email(message = "invalid e-mail")
    @Column(nullable = false)
    private String email;

    @Schema(description = "Date of birth", example = "1998-05-20")
    @Column(name = "birth_date")
    private LocalDate birthDate;

    // Composição: a Trip não existe sem o Passenger
    @Schema(description = "trips Passenger", accessMode = Schema.AccessMode.READ_ONLY)
    @OneToMany(mappedBy = "passenger", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("passenger")
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
    public List<Trip> getTrips() {
        return trips;
    }
    public void setTrips(List<Trip> trips) {
        this.trips = trips;
    }

    

    
}
