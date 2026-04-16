package com.notifyflow.coreprocessor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyflow.coreprocessor.entity.AuditLog;
import com.notifyflow.coreprocessor.model.CustomerEvent;
import com.notifyflow.coreprocessor.model.EnrichedEvent;
import com.notifyflow.coreprocessor.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AuditLogService auditLogService;

    private CustomerEvent customerEvent;
    private EnrichedEvent enrichedEvent;

    @BeforeEach
    void setUp() {
        customerEvent = CustomerEvent.builder()
                .eventId("EVT-001")
                .customerId("CUST-123")
                .notificationType("PROMOTIONAL")
                .subject("Test subject")
                .body("Test body")
                .build();

        enrichedEvent = EnrichedEvent.builder()
                .eventId("EVT-001")
                .customerId("CUST-123")
                .customerName("John Doe")
                .customerEmail("john@example.com")
                .notificationType("PROMOTIONAL")
                .build();
    }

    @Test
    void logReceived_shouldSaveAuditLogWithReceivedStatus() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        auditLogService.logReceived(customerEvent);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals("EVT-001", saved.getEventId());
        assertEquals("CUST-123", saved.getCustomerId());
        assertEquals("RECEIVED", saved.getStatus());
        assertEquals("PROMOTIONAL", saved.getNotificationType());
    }

    @Test
    void logRouted_shouldUpdateAuditLogWithRoutedStatus() {
        AuditLog existing = AuditLog.builder()
                .eventId("EVT-001")
                .customerId("CUST-123")
                .status("RECEIVED")
                .build();

        when(auditLogRepository.findByEventId("EVT-001"))
                .thenReturn(Optional.of(existing));
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        auditLogService.logRouted(customerEvent, enrichedEvent, "EMAIL");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        AuditLog updated = captor.getValue();
        assertEquals("ROUTED", updated.getStatus());
        assertEquals("EMAIL", updated.getRoutedToChannel());
    }

    @Test
    void logFailed_shouldUpdateAuditLogWithFailedStatus() {
        AuditLog existing = AuditLog.builder()
                .eventId("EVT-001")
                .customerId("CUST-123")
                .status("RECEIVED")
                .build();

        when(auditLogRepository.findByEventId("EVT-001"))
                .thenReturn(Optional.of(existing));
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        auditLogService.logFailed(customerEvent, "Connection refused");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        AuditLog updated = captor.getValue();
        assertEquals("FAILED", updated.getStatus());
        assertEquals("Connection refused", updated.getFailureReason());
    }

    @Test
    void isDuplicate_shouldReturnTrue_whenEventIdExists() {
        when(auditLogRepository.existsByEventId("EVT-001")).thenReturn(true);

        boolean result = auditLogService.isDuplicate("EVT-001");

        assertTrue(result);
        verify(auditLogRepository, times(1)).existsByEventId("EVT-001");
    }

    @Test
    void isDuplicate_shouldReturnFalse_whenEventIdDoesNotExist() {
        when(auditLogRepository.existsByEventId("EVT-NEW")).thenReturn(false);

        boolean result = auditLogService.isDuplicate("EVT-NEW");

        assertFalse(result);
    }

}