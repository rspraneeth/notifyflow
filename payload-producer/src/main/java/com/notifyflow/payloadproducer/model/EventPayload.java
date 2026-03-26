package com.notifyflow.payloadproducer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventPayload {

    private String eventId;
    private String customerId;
    private String notificationType;
    private String subject;
    private String body;
    private String channel;

}
