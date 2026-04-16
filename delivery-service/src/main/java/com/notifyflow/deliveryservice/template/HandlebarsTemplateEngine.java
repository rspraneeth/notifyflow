package com.notifyflow.deliveryservice.template;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@Slf4j
public class HandlebarsTemplateEngine {

    private final Handlebars handlebars = new Handlebars();

    public String render(String templateContent, Map<String, Object> data) {
        try {
            Template template = handlebars.compileInline(templateContent);
            return template.apply(data);
        } catch (IOException e) {
            log.error("Failed to render template | error: {}", e.getMessage());
            return templateContent;
        }
    }

}