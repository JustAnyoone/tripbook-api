package com.tt.tripbook.exception;

import java.time.LocalDateTime;

// Formato padrão das respostas de erro da API
public record ApiError(int status, String mensagem, LocalDateTime timestamp) {

    public ApiError(int status, String mensagem) {
        this(status, mensagem, LocalDateTime.now());
    }
}
