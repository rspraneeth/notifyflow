package com.notifyflow.coreprocessor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CustomerProfile {

    private String customerId;
    private String customerName;
    private String email;
    private String phone;
}
