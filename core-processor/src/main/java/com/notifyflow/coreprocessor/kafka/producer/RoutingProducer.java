package com.notifyflow.coreprocessor.kafka.producer;


import com.notifyflow.coreprocessor.model.EnrichedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoutingProducer {

    private final KafkaTemplate<String, EnrichedEvent> kafkaTemplate;

    @Value("${kafka.topic.email-events}")
    private String emailTopic;

    @Value("${kafka.topic.sms-events}")
    private String smsTopic;

    @Value("${kafka.topic.push-events}")
    private String pushTopic;

    @Value("${kafka.topic.dead-letter}")
    private String deadLetterTopic;

    public void publishToEmail(EnrichedEvent event) {
        log.info("Publishing to email topic | eventId: {}", event.getEventId());
        kafkaTemplate.send(emailTopic, event.getCustomerId(), event);
    }

    public void publishToSms(EnrichedEvent event) {
        log.info("Publishing to SMS topic | eventId: {}", event.getEventId());
        kafkaTemplate.send(smsTopic, event.getCustomerId(), event);
    }

    public void publishToPush(EnrichedEvent event) {
        log.info("Publishing to push topic | eventId: {}", event.getEventId());
        kafkaTemplate.send(pushTopic, event.getCustomerId(), event);
    }

    public void publishToDeadLetter(EnrichedEvent event) {
        log.warn("Publishing to dead letter queue | eventId: {}", event.getEventId());
        kafkaTemplate.send(deadLetterTopic, event.getCustomerId(), event);
    }
}
