package com.notifyflow.deliveryservice.handler;

import com.notifyflow.deliveryservice.model.EnrichedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PushHandler {

    public void handle(EnrichedEvent event) {
        log.info("Processing push notification | eventId: {} | customerId: {}",
                event.getEventId(), event.getCustomerId());

        // In production this would call Firebase FCM, APNs, or AWS Pinpoint
        log.info("Push notification dispatched | eventId: {} | subject: {} | body: {}",
                event.getEventId(), event.getSubject(), event.getBody());
    }

}