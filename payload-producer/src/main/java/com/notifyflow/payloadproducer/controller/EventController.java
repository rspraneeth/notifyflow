package com.notifyflow.payloadproducer.controller;

import com.notifyflow.payloadproducer.model.EventPayload;
import com.notifyflow.payloadproducer.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
@Slf4j
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping("/publish")
    public ResponseEntity<String> publishEvent(@RequestBody EventPayload payload){
        log.info("Received event request | eventId: {} | customerId: {}", payload.getEventId(), payload.getCustomerId());
        eventService.processAndPublish(payload);
        return ResponseEntity.ok("Event published successfully");
    }

    @GetMapping("/health")
    public ResponseEntity<String> health(){
        return ResponseEntity.ok("payload-producer is running");
    }
}
