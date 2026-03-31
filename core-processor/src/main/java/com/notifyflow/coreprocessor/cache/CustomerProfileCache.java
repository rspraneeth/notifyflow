package com.notifyflow.coreprocessor.cache;

import com.notifyflow.coreprocessor.model.CustomerProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CustomerProfileCache {

    @Cacheable(value = "customerProfiles", key = "#customerId")
    public CustomerProfile getProfile(String customerId){
        log.info("Cache miss - fetching profile from API | customerId: {}", customerId);
        return fetchFromApi(customerId);
    }

    private CustomerProfile fetchFromApi(String customerId){
        log.info("Calling Customer Profile API | customerId: {}", customerId);

        return CustomerProfile.builder()
                .customerId(customerId)
                .customerName("Customer " + customerId)
                .email(customerId.toLowerCase() + "@example.com")
                .phone("+1-555-000-" + customerId)
                .build();
    }
}
