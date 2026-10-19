package com.tt.tripbook.model;

// Lifecycle of a trip booking. Stored as text (EnumType.STRING), so the existing values keep working.
public enum TripStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
