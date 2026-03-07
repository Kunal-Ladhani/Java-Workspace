package com.learn.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.kafka.model.Subscription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Producer class to send Subscription records to Kafka topic
 * Uses subscription ID as the message key
 */
@Component
@Slf4j
public class SubscriptionProducer {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;
    
    public SubscriptionProducer(KafkaTemplate<String, String> kafkaTemplate, 
                                ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = "subscriptions";
    }
    
    /**
     * Produces a subscription record to Kafka topic
     * Uses subscription.getId() as the message key
     */
    public void produce(Subscription subscription) {
        try {
            String value = objectMapper.writeValueAsString(subscription);
            String key = subscription.getId();
            
            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(topic, key, value);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Sent subscription with key={} to topic={}, partition={}, offset={}",
                        key, topic, result.getRecordMetadata().partition(), 
                        result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send subscription with key={}", key, ex);
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Error serializing subscription: {}", subscription.getId(), e);
        }
    }
}
