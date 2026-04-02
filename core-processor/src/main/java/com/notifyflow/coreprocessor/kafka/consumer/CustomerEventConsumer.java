package com.notifyflow.coreprocessor.kafka.consumer;

import com.notifyflow.coreprocessor.model.CustomerEvent;
import com.notifyflow.coreprocessor.model.EnrichedEvent;
import com.notifyflow.coreprocessor.service.AuditLogService;
import com.notifyflow.coreprocessor.service.EnrichmentService;
import com.notifyflow.coreprocessor.service.RoutingService;
import com.notifyflow.coreprocessor.service.ValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CustomerEventConsumer {

    private final ValidationService validationService;
    private final EnrichmentService enrichmentService;
    private final AuditLogService auditLogService;
    private final RoutingService routingService;

    @KafkaListener(
            topics = "${kafka.topic.customer-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(
            @Payload CustomerEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received message | topic: {} | partition: {} | offset: {} | eventId: {}",
                topic, partition, offset, event.getEventId());

        try {
            // Step 1 - Idempotency check
            if (auditLogService.isDuplicate(event.getEventId())) {
                log.warn("Duplicate event detected, skipping | eventId: {}",
                        event.getEventId());
                return;
            }

            // Step 2 - Log received
            auditLogService.logReceived(event);

            // Step 3 - Validate
            validationService.validate(event);

            // Step 4 - Enrich
            EnrichedEvent enrichedEvent = enrichmentService.enrich(event);

            // Step 5 - Route
            String channel = routingService.route(enrichedEvent);

            // Step 6 - Log routed
            auditLogService.logRouted(event, enrichedEvent, channel);

            log.info("Event processed successfully | eventId: {} | channel: {}",
                    event.getEventId(), channel);

        } catch (Exception e) {
            log.error("Error processing event | eventId: {} | error: {}",
                    event.getEventId(), e.getMessage());
            auditLogService.logFailed(event, e.getMessage());
        }

    }

}