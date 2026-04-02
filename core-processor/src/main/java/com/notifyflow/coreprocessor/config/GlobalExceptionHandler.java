package com.notifyflow.coreprocessor.config;

import com.notifyflow.coreprocessor.exception.CustomerProfileNotFoundException;
import com.notifyflow.coreprocessor.exception.PayloadValidationException;
import com.notifyflow.coreprocessor.exception.RoutingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(PayloadValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            PayloadValidationException ex,
            HttpServletRequest request) {

        log.error("Validation error | path: {} | message: {}",
                request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildError(400, "Validation Failed",
                        ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(CustomerProfileNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCustomerNotFound(
            CustomerProfileNotFoundException ex,
            HttpServletRequest request) {

        log.error("Customer not found | path: {} | message: {}",
                request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildError(404, "Customer Not Found",
                        ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(RoutingException.class)
    public ResponseEntity<Map<String, Object>> handleRouting(
            RoutingException ex,
            HttpServletRequest request) {

        log.error("Routing error | path: {} | message: {}",
                request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(buildError(422, "Routing Failed",
                        ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected error | path: {} | message: {}",
                request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildError(500, "Internal Server Error",
                        "An unexpected error occurred",
                        request.getRequestURI()));
    }

    private Map<String, Object> buildError(int status, String error,
                                           String message, String path) {
        return Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status,
                "error", error,
                "message", message,
                "path", path
        );
    }

}