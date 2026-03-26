package com.notifyflow.payloadproducer.kafka;

import com.notifyflow.payloadproducer.model.EventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, EventPayload> kafkaTemplate;

    @Value("${kafka.topic.customer-events}")
    private String topic;

    public void publishEvent(EventPayload payload){
        log.info("Publishing event to Kafka topic: {} | eventId: {}", topic, payload.getEventId());
        kafkaTemplate.send(topic, payload.getCustomerId(), payload);
        log.info("Event published successfully | eventId: {}", payload.getEventId());
    }
}
