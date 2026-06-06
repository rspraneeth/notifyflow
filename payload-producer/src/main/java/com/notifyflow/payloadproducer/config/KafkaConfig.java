package com.notifyflow.payloadproducer.config;

import com.notifyflow.payloadproducer.model.EventPayload;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.properties.security.protocol:PLAINTEXT}")
    private String securityProtocol;

    @Value("${spring.kafka.properties.sasl.mechanism:GSSAPI}")
    private String saslMechanism;

    @Value("${spring.kafka.properties.sasl.jaas.config:}")
    private String saslJaasConfig;

    @Value("${spring.kafka.properties.ssl.truststore.location:}")
    private String truststoreLocation;

    @Value("${spring.kafka.properties.ssl.truststore.password:}")
    private String truststorePassword;

    @Value("${spring.kafka.properties.ssl.truststore.type:JKS}")
    private String truststoreType;

    @Value("${spring.kafka.properties.ssl.endpoint.identification.algorithm:https}")
    private String sslEndpointIdentificationAlgorithm;

    @Bean
    public ProducerFactory<String, EventPayload> producerFactory() {
        Map<String, Object> config = new HashMap<>();

        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        config.put("security.protocol", securityProtocol);
        config.put("sasl.mechanism", saslMechanism);
        if (!saslJaasConfig.isEmpty()) {
            config.put("sasl.jaas.config", saslJaasConfig);
        }
        if (!truststoreLocation.isEmpty()) {
            config.put("ssl.truststore.location", truststoreLocation);
            config.put("ssl.truststore.password", truststorePassword);
            config.put("ssl.truststore.type", truststoreType);
        }
        config.put("ssl.endpoint.identification.algorithm", sslEndpointIdentificationAlgorithm);

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, EventPayload> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}