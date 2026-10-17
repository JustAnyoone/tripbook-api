package com.tt.tripbook.exception;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

        // Corpo ilegível: JSON malformado ou valor inválido (ex.: data "2026-13-45")
        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ApiError> unreadableBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(400, "Malformed JSON or invalid value in the request body (dates must use yyyy-MM-dd)"));
        }

        // Parâmetro de rota ou de query com tipo errado (ex.: /api/trips/abc)
        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<ApiError> wrongParameterType(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(400, "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'"));
        }
        
        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ApiError> notFound(ResourceNotFoundException ex) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiError(404, ex.getMessage()));
        }

        @ExceptionHandler(ConflictException.class)
        public ResponseEntity<ApiError> conflict(ConflictException ex) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ApiError(409, ex.getMessage()));
        }

        // Dados de entrada inválidos (@Valid): junta as mensagens de todos os campos
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
                String message = ex.getBindingResult().getFieldErrors().stream()
                        .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                        .collect(Collectors.joining("; "));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiError(400, message));
        }

        // Rede de segurança: violação de chave estrangeira ou de campo único que ninguém tratou antes
        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ApiError> integrity(DataIntegrityViolationException ex) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ApiError(409, "Operation refused: the record is in use or violates a data constraint."));
        }

        // Unknown property in ?sort=... (e.g. sort=banana): a client error, not a server error
        @ExceptionHandler(PropertyReferenceException.class)
        public ResponseEntity<ApiError> invalidSortProperty(PropertyReferenceException ex) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiError(400, "Invalid sort property: '" + ex.getPropertyName() + "'"));
        }

        @ExceptionHandler(BusinessRuleException.class)
        public ResponseEntity<ApiError> BusinessRule(BusinessRuleException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(400, ex.getMessage()));
        }
}
