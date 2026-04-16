package com.notifyflow.deliveryservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrichedEvent {

    private String eventId;
    private String customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String notificationType;
    private String subject;
    private String body;
    private String channel;

}