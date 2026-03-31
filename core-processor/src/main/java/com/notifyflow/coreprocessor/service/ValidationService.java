package com.notifyflow.coreprocessor.service;

import com.notifyflow.coreprocessor.exception.PayloadValidationException;
import com.notifyflow.coreprocessor.model.CustomerEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ValidationService {

    public void validate(CustomerEvent event){

        log.info("Validating event | eventId: {}", event.getEventId());

        if (event.getEventId() == null || event.getEventId().isBlank()) {
            throw new PayloadValidationException("eventId is required");
        }
        if (event.getCustomerId() == null || event.getCustomerId().isBlank()) {
            throw new PayloadValidationException("customerId is required");
        }
        if (event.getNotificationType() == null || event.getNotificationType().isBlank()) {
            throw new PayloadValidationException("notificationType is required");
        }

        log.info("Validation passed | eventId: {}", event.getEventId());
    }
}
