package com.tt.tripbook.exception;

import java.time.LocalDateTime;

// Standard format of every error response of the API
public record ApiError(int status, String message, LocalDateTime timestamp) {

    public ApiError(int status, String message) {
        this(status, message, LocalDateTime.now());
    }
}
