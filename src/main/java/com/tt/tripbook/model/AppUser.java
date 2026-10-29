package com.tt.tripbook.model;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

// A person who can sign in to the API. Not related to Passenger: a passenger is a travel record,
// a user is an account.
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    // BCrypt hash, never the raw password
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // Default USER; ADMIN can manage shared resources (destinations)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @ColumnDefault("'USER'")
    private Role role = Role.USER;

    // Cloudinary URL of the account's profile picture. Null means no picture uploaded yet.
    @Column(name = "photo_url")
    private String photoUrl;

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}
