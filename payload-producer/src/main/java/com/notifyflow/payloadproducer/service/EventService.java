package com.notifyflow.payloadproducer.service;

import com.notifyflow.payloadproducer.kafka.KafkaProducerService;
import com.notifyflow.payloadproducer.model.EventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventService {

    private final KafkaProducerService kafkaProducerService;

    public void processAndPublish(EventPayload payload){
        validatePayload(payload);
        kafkaProducerService.publishEvent(payload);
    }

    private void validatePayload(EventPayload payload){
         if (payload.getEventId() == null || payload.getEventId().isBlank()) throw new IllegalArgumentException("EventId is required");
         if (payload.getCustomerId() == null || payload.getCustomerId().isBlank()) throw new IllegalArgumentException("CustomerId is required");
         if (payload.getNotificationType() == null || payload.getNotificationType().isBlank()) throw new IllegalArgumentException("NotificationType is required");
    }
}
