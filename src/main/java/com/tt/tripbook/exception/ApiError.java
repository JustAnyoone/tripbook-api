package com.tt.tripbook.exception;

import java.time.LocalDateTime;

// Formato padrão das respostas de erro da API
public record ApiError(int status, String message, LocalDateTime timestamp) {

    public ApiError(int status, String message) {
        this(status, message, LocalDateTime.now());
    }
}
