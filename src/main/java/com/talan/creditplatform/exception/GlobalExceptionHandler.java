package com.talan.creditplatform.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SoleAdminException.class)
    public ResponseEntity<Map<String, String>> handleSoleAdmin(SoleAdminException ex) {
        return ResponseEntity.status(409).body(Map.of("message", ex.getMessage()));
    }
}
