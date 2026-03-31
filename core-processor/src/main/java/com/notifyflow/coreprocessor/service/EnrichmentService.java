package com.notifyflow.coreprocessor.service;

import com.notifyflow.coreprocessor.cache.CustomerProfileCache;
import com.notifyflow.coreprocessor.exception.CustomerProfileNotFoundException;
import com.notifyflow.coreprocessor.model.CustomerEvent;
import com.notifyflow.coreprocessor.model.CustomerProfile;
import com.notifyflow.coreprocessor.model.EnrichedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EnrichmentService {

    private final CustomerProfileCache customerProfileCache;

    public EnrichedEvent enrich(CustomerEvent event){
        log.info("Enriching event | eventId: {} | customerId: {}", event.getEventId(), event.getCustomerId());

        CustomerProfile profile = customerProfileCache.getProfile(event.getCustomerId());

        if (profile == null){
            throw new CustomerProfileNotFoundException(event.getCustomerId());
        }

        EnrichedEvent enriched = EnrichedEvent.builder()
                .eventId(event.getEventId())
                .customerId(event.getCustomerId())
                .customerName(profile.getCustomerName())
                .customerEmail(profile.getEmail())
                .customerPhone(profile.getPhone())
                .notificationType(event.getNotificationType())
                .subject(event.getSubject())
                .body(event.getBody())
                .channel(event.getChannel())
                .build();

        log.info("Enrichment complete | eventId: {} | customerName: {}", event.getEventId(), profile.getCustomerName());

        return enriched;
    }
}
