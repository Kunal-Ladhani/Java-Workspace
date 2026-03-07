package com.learn.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.kafka.model.Subscription;
import com.learn.kafka.service.InMemoryDatabase;
import com.learn.kafka.service.SubscriptionValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Properties;

/**
 * Consumer that polls for 3 seconds per iteration and stops after 10 seconds total
 * Uses synchronous mode (manual commit)
 */
@Component
@Slf4j
public class SubscriptionConsumer implements Runnable {
    
    private final KafkaConsumer<String, String> consumer;
    private final ObjectMapper objectMapper;
    private final SubscriptionValidator validator;
    private final InMemoryDatabase database;
    
    private static final String TOPIC = "subscriptions";
    private static final long POLL_DURATION_SECONDS = 3;
    private static final long TOTAL_DURATION_SECONDS = 10;
    
    public SubscriptionConsumer(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
                                ObjectMapper objectMapper,
                                SubscriptionValidator validator,
                                InMemoryDatabase database) {
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.database = database;
        
        // Configure consumer properties
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("group.id", "subscription-consumer-group");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("enable.auto.commit", "false"); // Synchronous mode - manual commit
        props.put("auto.offset.reset", "earliest");
        
        this.consumer = new KafkaConsumer<>(props);
        this.consumer.subscribe(Collections.singletonList(TOPIC));
    }
    
    /**
     * Run method that implements the consumer logic:
     * - Polls for 3 seconds per iteration
     * - Stops consuming after 10 seconds total
     * - Uses synchronous mode (manual commit)
     * - Validates records and saves valid ones to database
     */
    @Override
    public void run() {
        log.info("Starting consumer - will poll for {}s per iteration, stopping after {}s total", 
            POLL_DURATION_SECONDS, TOTAL_DURATION_SECONDS);
        
        Instant startTime = Instant.now();
        int totalProcessed = 0;
        int totalValidated = 0;
        
        try {
            while (true) {
                // Check if 10 seconds have elapsed
                Instant currentTime = Instant.now();
                long elapsedSeconds = Duration.between(startTime, currentTime).getSeconds();
                
                if (elapsedSeconds >= TOTAL_DURATION_SECONDS) {
                    log.info("Total duration of {}s reached. Stopping consumer.", TOTAL_DURATION_SECONDS);
                    break;
                }
                
                // Calculate remaining time for this poll
                long remainingSeconds = TOTAL_DURATION_SECONDS - elapsedSeconds;
                long pollDurationMs = Math.min(POLL_DURATION_SECONDS * 1000, remainingSeconds * 1000);
                
                if (pollDurationMs <= 0) {
                    break;
                }
                
                log.debug("Polling for {}ms (elapsed: {}s, remaining: {}s)", 
                    pollDurationMs, elapsedSeconds, remainingSeconds);
                
                // Poll for records (3 seconds or remaining time, whichever is less)
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(pollDurationMs));
                
                if (records.isEmpty()) {
                    log.debug("No records received in this poll");
                    continue;
                }
                
                log.info("Received {} records in this poll", records.count());
                
                // Process each record
                for (ConsumerRecord<String, String> record : records) {
                    totalProcessed++;
                    
                    try {
                        // Deserialize the subscription
                        Subscription subscription = objectMapper.readValue(record.value(), Subscription.class);
                        
                        // Validate the subscription
                        if (validator.validate(subscription)) {
                            // Save to database
                            database.save(subscription);
                            totalValidated++;
                            log.info("Validated and saved subscription: id={}, userId={}", 
                                subscription.getId(), subscription.getUserId());
                        } else {
                            log.warn("Validation failed for subscription: id={}", 
                                subscription.getId() != null ? subscription.getId() : "null");
                        }
                    } catch (Exception e) {
                        log.error("Error processing record with key={}, offset={}", 
                            record.key(), record.offset(), e);
                    }
                }
                
                // Commit offsets synchronously (synchronous mode)
                try {
                    consumer.commitSync();
                    log.debug("Committed offsets synchronously");
                } catch (Exception e) {
                    log.error("Error committing offsets", e);
                }
                
                // Check again if we've exceeded the time limit after processing
                currentTime = Instant.now();
                elapsedSeconds = Duration.between(startTime, currentTime).getSeconds();
                if (elapsedSeconds >= TOTAL_DURATION_SECONDS) {
                    log.info("Total duration of {}s reached after processing. Stopping consumer.", 
                        TOTAL_DURATION_SECONDS);
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Error in consumer run method", e);
        } finally {
            log.info("Consumer finished. Total processed: {}, Total validated and saved: {}", 
                totalProcessed, totalValidated);
            consumer.close();
            log.info("Consumer closed");
        }
    }
}
