package com.notifyflow.coreprocessor.cache;

import com.notifyflow.coreprocessor.model.CustomerProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
@RequiredArgsConstructor
public class CustomerProfileCache {

    public final RestTemplate restTemplate;

    @Value("${customer.service.url}")
    private String customerServiceUrl;

    @Cacheable(value = "customerProfiles", key = "#customerId")
    public CustomerProfile getProfile(String customerId){
        log.info("Cache miss - fetching profile from API | customerId: {}", customerId);
        return fetchFromApi(customerId);
    }

    private CustomerProfile fetchFromApi(String customerId){
        log.info("Calling Customer Profile API | customerId: {}", customerId);

        String url = customerServiceUrl + "/api/customers/" + customerId;

        CustomerProfile profile = restTemplate.getForObject(url,
                CustomerProfile.class);

        log.info("Customer profile fetched | customerId: {} | name: {}",
                customerId, profile != null ? profile.getCustomerName() : "null");

        return profile;
    }
}
