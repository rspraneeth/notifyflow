package com.notifyflow.deliveryservice.handler;

import com.notifyflow.deliveryservice.model.EnrichedEvent;
import com.notifyflow.deliveryservice.template.HandlebarsTemplateEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmailHandler {

    private final JavaMailSender mailSender;
    private final HandlebarsTemplateEngine templateEngine;

    @Value("${mail.from}")
    private String fromEmail;

    public void handle(EnrichedEvent event) {
        log.info("Processing email delivery | eventId: {} | to: {}",
                event.getEventId(), event.getCustomerEmail());

        Map<String, Object> data = Map.of(
                "customerName", event.getCustomerName(),
                "customerId", event.getCustomerId(),
                "subject", event.getSubject() != null ? event.getSubject() : "",
                "notificationType", event.getNotificationType()
        );

        String renderedBody = templateEngine.render(event.getBody(), data);
        String renderedSubject = templateEngine.render(event.getSubject(), data);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(event.getCustomerEmail());
        message.setSubject(renderedSubject);
        message.setText(renderedBody);

        mailSender.send(message);

        log.info("Email sent successfully | eventId: {} | to: {}", event.getEventId(), event.getCustomerEmail());
    }

}