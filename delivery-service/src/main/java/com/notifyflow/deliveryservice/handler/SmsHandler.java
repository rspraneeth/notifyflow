package com.notifyflow.deliveryservice.handler;

import com.notifyflow.deliveryservice.model.EnrichedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SmsHandler {

    public void handle(EnrichedEvent event) {
        log.info("Processing SMS delivery | eventId: {} | to: {} | phone: {}",
                event.getEventId(), event.getCustomerId(), event.getCustomerPhone());

        // In production this would call Twilio, AWS SNS, or similar SMS provider
        log.info("SMS dispatched | eventId: {} | phone: {} | message: {}",
                event.getEventId(), event.getCustomerPhone(), event.getBody());
    }

}