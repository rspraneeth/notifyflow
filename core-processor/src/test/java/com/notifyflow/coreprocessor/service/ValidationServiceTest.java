package com.notifyflow.coreprocessor.service;

import com.notifyflow.coreprocessor.exception.PayloadValidationException;
import com.notifyflow.coreprocessor.model.CustomerEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationServiceTest {

    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new ValidationService();
    }

    @Test
    void validate_shouldPass_whenAllFieldsPresent() {
        CustomerEvent event = CustomerEvent.builder()
                .eventId("EVT-001")
                .customerId("CUST-123")
                .notificationType("PROMOTIONAL")
                .subject("Test subject")
                .body("Test body")
                .build();

        assertDoesNotThrow(() -> validationService.validate(event));
    }

    @Test
    void validate_shouldThrow_whenEventIdIsNull() {
        CustomerEvent event = CustomerEvent.builder()
                .customerId("CUST-123")
                .notificationType("PROMOTIONAL")
                .build();

        PayloadValidationException exception = assertThrows(
                PayloadValidationException.class,
                () -> validationService.validate(event)
        );

        assertEquals("eventId is required", exception.getMessage());
    }

    @Test
    void validate_shouldThrow_whenEventIdIsBlank() {
        CustomerEvent event = CustomerEvent.builder()
                .eventId("   ")
                .customerId("CUST-123")
                .notificationType("PROMOTIONAL")
                .build();

        assertThrows(PayloadValidationException.class,
                () -> validationService.validate(event));
    }

    @Test
    void validate_shouldThrow_whenCustomerIdIsNull() {
        CustomerEvent event = CustomerEvent.builder()
                .eventId("EVT-001")
                .notificationType("PROMOTIONAL")
                .build();

        PayloadValidationException exception = assertThrows(
                PayloadValidationException.class,
                () -> validationService.validate(event)
        );

        assertEquals("customerId is required", exception.getMessage());
    }

    @Test
    void validate_shouldThrow_whenNotificationTypeIsNull() {
        CustomerEvent event = CustomerEvent.builder()
                .eventId("EVT-001")
                .customerId("CUST-123")
                .build();

        PayloadValidationException exception = assertThrows(
                PayloadValidationException.class,
                () -> validationService.validate(event)
        );

        assertEquals("notificationType is required", exception.getMessage());
    }

}