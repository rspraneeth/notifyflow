package com.notifyflow.coreprocessor.service;


import com.notifyflow.coreprocessor.exception.RoutingException;
import com.notifyflow.coreprocessor.kafka.producer.RoutingProducer;
import com.notifyflow.coreprocessor.model.EnrichedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoutingService {

    private final RoutingProducer routingProducer;

    public String route(EnrichedEvent event){
        log.info("Routing event | eventId: {} | notificationType: {}", event.getEventId(), event.getNotificationType());

        if (event.getNotificationType() == null){
            throw new RoutingException("notificationType is null for eventId: "+event.getEventId());
        }

        String channel = switch (event.getNotificationType().toUpperCase()) {
            case "PROMOTIONAL", "TRANSACTIONAL" -> {
                routingProducer.publishToEmail(event);
                yield "EMAIL";
            }
            case "ALERT" -> {
                routingProducer.publishToPush(event);
                yield "PUSH";
            }
            case "SMS" -> {
                routingProducer.publishToSms(event);
                yield "SMS";
            }
            default -> {
                log.warn("Unknown notificationType: {} | eventId: {} | routing to DLQ", event.getNotificationType(), event.getEventId());
                routingProducer.publishToDeadLetter(event);
                yield "DEAD_LETTER";
            }
        };

        log.info("Event routed successfully | eventId: {} | channel: {}", event.getEventId(), channel);

        return channel;
    }
}
