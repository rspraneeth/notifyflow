package com.notifyflow.coreprocessor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyflow.coreprocessor.entity.AuditLog;
import com.notifyflow.coreprocessor.model.CustomerEvent;
import com.notifyflow.coreprocessor.model.EnrichedEvent;
import com.notifyflow.coreprocessor.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    
    public void logReceived(CustomerEvent event){
        log.info("Logging received event | eventId: {}", event.getEventId());

        AuditLog auditLog = AuditLog.builder()
                .eventId(event.getEventId())
                .customerId(event.getCustomerId())
                .notificationType(event.getNotificationType())
                .status("RECEIVED")
                .rawPayload(toJson(event))
                .build();

        auditLogRepository.save(auditLog);
    }

    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON", e);
            return "{}";
        }
    }

    public void logRouted(CustomerEvent event, EnrichedEvent enrichedEvent, String channel){
        log.info("Logging routed event | eventId: {} | channel: {}", event.getEventId(), channel);

        auditLogRepository.findByEventId(event.getEventId())
                .ifPresentOrElse(
                        existing -> {
                            existing.setStatus("ROUTED");
                            existing.setRoutedToChannel(channel);
                            existing.setEnrichedPayload(toJson(enrichedEvent));
                            auditLogRepository.save(existing);
                        },
                        () -> log.warn("Audit log not found for eventId: {}", event.getEventId())
                );
    }


    public void logFailed(CustomerEvent event, String failureReason) {
        log.error("Logging failed event | eventId: {} | reason: {}", event.getEventId(), failureReason);

        auditLogRepository.findByEventId(event.getEventId())
                .ifPresentOrElse(
                        existing -> {
                            existing.setStatus("FAILED");
                            existing.setFailureReason(failureReason);
                            auditLogRepository.save(existing);
                        },
                        () -> {
                            AuditLog auditLog = AuditLog.builder()
                                    .eventId(event.getEventId())
                                    .customerId(event.getCustomerId())
                                    .notificationType(event.getNotificationType())
                                    .status("FAILED")
                                    .rawPayload(toJson(event))
                                    .failureReason(failureReason)
                                    .build();
                            auditLogRepository.save(auditLog);
                        }
                );
    }

    public boolean isDuplicate(String eventId) {
        return auditLogRepository.existsByEventId(eventId);
    }
}
