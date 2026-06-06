package com.notifyflow.customerservice.service;

import com.notifyflow.customerservice.model.CustomerProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CustomerService {

    public CustomerProfile getCustomerProfile(String customerId) {
        log.info("Fetching customer profile | customerId: {}", customerId);

        return CustomerProfile.builder()
                .customerId(customerId)
                .customerName(generateName(customerId))
                .email("rspraneeth.rsp@gmail.com")
                .phone("+1-555-" + customerId.replaceAll("[^0-9]", "0"))
                .build();
    }

    private String generateName(String customerId) {
        return switch (customerId.toUpperCase()) {
            case "CUST-123" -> "John Doe";
            case "CUST-456" -> "Jane Smith";
            case "CUST-789" -> "Bob Johnson";
            default -> "Customer " + customerId;
        };
    }
}
