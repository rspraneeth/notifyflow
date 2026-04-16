package com.notifyflow.coreprocessor.service;

import com.notifyflow.coreprocessor.exception.RoutingException;
import com.notifyflow.coreprocessor.kafka.producer.RoutingProducer;
import com.notifyflow.coreprocessor.model.EnrichedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoutingServiceTest {

    @Mock
    private RoutingProducer routingProducer;

    @InjectMocks
    private RoutingService routingService;

    private EnrichedEvent baseEvent;

    @BeforeEach
    void setUp() {
        baseEvent = EnrichedEvent.builder()
                .eventId("EVT-001")
                .customerId("CUST-123")
                .customerName("John Doe")
                .customerEmail("john@example.com")
                .customerPhone("+1-555-123")
                .subject("Test subject")
                .body("Test body")
                .build();
    }

    @Test
    void route_shouldRouteToEmail_whenNotificationTypeIsPromotional() {
        baseEvent.setNotificationType("PROMOTIONAL");

        String channel = routingService.route(baseEvent);

        assertEquals("EMAIL", channel);
        verify(routingProducer, times(1)).publishToEmail(baseEvent);
        verify(routingProducer, never()).publishToSms(any());
        verify(routingProducer, never()).publishToPush(any());
        verify(routingProducer, never()).publishToDeadLetter(any());
    }

    @Test
    void route_shouldRouteToEmail_whenNotificationTypeIsTransactional() {
        baseEvent.setNotificationType("TRANSACTIONAL");

        String channel = routingService.route(baseEvent);

        assertEquals("EMAIL", channel);
        verify(routingProducer, times(1)).publishToEmail(baseEvent);
    }

    @Test
    void route_shouldRouteToSms_whenNotificationTypeIsSms() {
        baseEvent.setNotificationType("SMS");

        String channel = routingService.route(baseEvent);

        assertEquals("SMS", channel);
        verify(routingProducer, times(1)).publishToSms(baseEvent);
        verify(routingProducer, never()).publishToEmail(any());
    }

    @Test
    void route_shouldRouteToPush_whenNotificationTypeIsAlert() {
        baseEvent.setNotificationType("ALERT");

        String channel = routingService.route(baseEvent);

        assertEquals("PUSH", channel);
        verify(routingProducer, times(1)).publishToPush(baseEvent);
        verify(routingProducer, never()).publishToEmail(any());
    }

    @Test
    void route_shouldRouteToDeadLetter_whenNotificationTypeIsUnknown() {
        baseEvent.setNotificationType("UNKNOWN_TYPE");

        String channel = routingService.route(baseEvent);

        assertEquals("DEAD_LETTER", channel);
        verify(routingProducer, times(1)).publishToDeadLetter(baseEvent);
        verify(routingProducer, never()).publishToEmail(any());
    }

    @Test
    void route_shouldBeCaseInsensitive() {
        baseEvent.setNotificationType("promotional");

        String channel = routingService.route(baseEvent);

        assertEquals("EMAIL", channel);
        verify(routingProducer, times(1)).publishToEmail(baseEvent);
    }

    @Test
    void route_shouldThrow_whenNotificationTypeIsNull() {
        baseEvent.setNotificationType(null);

        assertThrows(RoutingException.class,
                () -> routingService.route(baseEvent));

        verify(routingProducer, never()).publishToEmail(any());
        verify(routingProducer, never()).publishToDeadLetter(any());
    }

}