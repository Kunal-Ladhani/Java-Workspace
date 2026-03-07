package com.learn.kafka.runner;

import com.learn.kafka.model.Subscription;
import com.learn.kafka.producer.SubscriptionProducer;
import com.learn.kafka.consumer.SubscriptionConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Demo runner to demonstrate Kafka producer and consumer
 * Run this to see the consumer in action
 * Enable with: --spring.kafka.demo.enabled=true
 */
@Component
@ConditionalOnProperty(name = "spring.kafka.demo.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class KafkaDemoRunner implements CommandLineRunner {
    
    private final SubscriptionProducer producer;
    private final SubscriptionConsumer consumer;
    
    public KafkaDemoRunner(SubscriptionProducer producer, SubscriptionConsumer consumer) {
        this.producer = producer;
        this.consumer = consumer;
    }
    
    @Override
    public void run(String... args) throws Exception {
        log.info("=== Kafka Learning Demo ===");
        
        // Produce some test subscriptions
        log.info("Producing subscription records...");
        
        Subscription sub1 = new Subscription("sub1", "user1", "premium", 99.99, true);
        Subscription sub2 = new Subscription("sub2", "user2", "basic", 29.99, true);
        Subscription sub3 = new Subscription("sub3", "user3", "premium", 99.99, false);
        Subscription sub4 = new Subscription("sub4", "user4", "basic", 29.99, true);
        
        // Invalid subscription (will be rejected by validator)
        Subscription invalidSub = new Subscription("sub5", "user5", "premium", -10.0, true);
        
        producer.produce(sub1);
        producer.produce(sub2);
        producer.produce(sub3);
        producer.produce(sub4);
        producer.produce(invalidSub);
        
        // Give producer time to send messages
        Thread.sleep(1000);
        
        log.info("Starting consumer...");
        log.info("Consumer will:");
        log.info("  - Poll for 3 seconds per iteration");
        log.info("  - Stop after 10 seconds total");
        log.info("  - Use synchronous mode (manual commit)");
        log.info("  - Validate records and save valid ones to database");
        
        // Run consumer in a separate thread
        Thread consumerThread = new Thread(consumer);
        consumerThread.start();
        consumerThread.join(12000); // Wait for consumer to finish
        
        log.info("=== Demo Complete ===");
    }
}
