package com.notifyflow.customerservice.controller;

import com.notifyflow.customerservice.model.CustomerProfile;
import com.notifyflow.customerservice.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/customers")
@Slf4j
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerProfile> getCustomer(@PathVariable String customerId){
        log.info("Customer profile request received | customerId: {}", customerId);
        CustomerProfile profile = customerService.getCustomerProfile(customerId);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("customer-service is running");
    }
}
