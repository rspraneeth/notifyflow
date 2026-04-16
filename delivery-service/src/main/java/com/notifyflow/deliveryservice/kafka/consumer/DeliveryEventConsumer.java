package com.notifyflow.deliveryservice.kafka.consumer;

import com.notifyflow.deliveryservice.handler.EmailHandler;
import com.notifyflow.deliveryservice.handler.PushHandler;
import com.notifyflow.deliveryservice.handler.SmsHandler;
import com.notifyflow.deliveryservice.model.EnrichedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
@RequiredArgsConstructor
public class DeliveryEventConsumer {

    private final EmailHandler emailHandler;
    private final SmsHandler smsHandler;
    private final PushHandler pushHandler;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${kafka.topic.email-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeEmail(
            @Payload byte[] payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {
        try {
            EnrichedEvent event = objectMapper.readValue(payload, EnrichedEvent.class);
            log.info("Received email event | topic: {} | offset: {} | eventId: {}",
                    topic, offset, event.getEventId());
            emailHandler.handle(event);
        } catch (Exception e) {
            log.error("Failed to process email event | error: {}", e.getMessage());
        }
    }

    @KafkaListener(
            topics = "${kafka.topic.sms-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeSms(
            @Payload byte[] payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {
        try {
            EnrichedEvent event = objectMapper.readValue(payload, EnrichedEvent.class);
            log.info("Received SMS event | topic: {} | offset: {} | eventId: {}",
                    topic, offset, event.getEventId());
            smsHandler.handle(event);
        } catch (Exception e) {
            log.error("Failed to process SMS event | error: {}", e.getMessage());
        }
    }

    @KafkaListener(
            topics = "${kafka.topic.push-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumePush(
            @Payload byte[] payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {
        try {
            EnrichedEvent event = objectMapper.readValue(payload, EnrichedEvent.class);
            log.info("Received push event | topic: {} | offset: {} | eventId: {}",
                    topic, offset, event.getEventId());
            pushHandler.handle(event);
        } catch (Exception e) {
            log.error("Failed to process push event | error: {}", e.getMessage());
        }
    }

}