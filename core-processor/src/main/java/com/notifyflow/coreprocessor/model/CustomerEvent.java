package com.notifyflow.coreprocessor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CustomerEvent {

    private String eventId;
    private String customerId;
    private String notificationType;
    private String subject;
    private String body;
    private String channel;
}
