package com.communex.common;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        // A null-message IllegalArgumentException usually indicates an unexpected
        // backend bug. Log the stack trace instead of returning {"error":null}.
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            log.error("IllegalArgumentException without a message", ex);
            Map<String, String> response = Map.of(
                "error", "Unexpected invalid argument in the backend. Check the backend console for the stack trace."
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
        log.warn("Request rejected as invalid: {}", message, ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleStatusException(ResponseStatusException ex) {
        String message = ex.getReason();
        if (message == null || message.isBlank()) message = "Request could not be completed.";
        if (ex.getStatusCode().is5xxServerError()) log.warn("Service request failed: {}", message, ex);
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error", message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> response = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            response.put(error.getField(), error.getDefaultMessage() == null
                ? "Invalid value" : error.getDefaultMessage()));
        if (response.isEmpty()) response.put("error", "Request validation failed.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
