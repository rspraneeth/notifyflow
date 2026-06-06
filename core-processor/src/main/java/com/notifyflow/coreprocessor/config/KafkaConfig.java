package com.notifyflow.coreprocessor.config;

import com.notifyflow.coreprocessor.model.CustomerEvent;
import com.notifyflow.coreprocessor.model.EnrichedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

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

    // Consumer configuration

    @Bean
    public ConsumerFactory<String, CustomerEvent> consumerFactory() {
        Map<String, Object> config = new HashMap<>();

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

        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, CustomerEvent.class.getName());
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CustomerEvent>
    kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, CustomerEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    // Producer configuration
    @Bean
    public ProducerFactory<String, EnrichedEvent> producerFactory() {
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
    public KafkaTemplate<String, EnrichedEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }

}