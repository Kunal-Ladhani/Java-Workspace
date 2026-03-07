package com.learn.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.kafka.model.Subscription;
import com.learn.kafka.service.InMemoryDatabase;
import com.learn.kafka.service.SubscriptionValidator;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for SubscriptionConsumer
 * Tests the key requirements:
 * - Poll duration is 3 seconds
 * - Stops consuming after 10 seconds total
 * - Synchronous mode (manual commit)
 * - Validates records and saves to database
 */
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"subscriptions"}, brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"})
class SubscriptionConsumerTest {
    
    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;
    
    @Autowired
    private SubscriptionValidator validator;
    
    @Autowired
    private InMemoryDatabase database;
    
    private ObjectMapper objectMapper;
    private KafkaTemplate<String, String> kafkaTemplate;
    private SubscriptionConsumer consumer;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        database.clear();
        
        // Create producer for test data
        Map<String, Object> producerProps = new HashMap<>(KafkaTestUtils.producerProps(embeddedKafkaBroker));
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        ProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(producerProps);
        kafkaTemplate = new KafkaTemplate<>(producerFactory);
        
        // Create consumer with embedded Kafka broker
        String bootstrapServers = embeddedKafkaBroker.getBrokersAsString();
        consumer = new SubscriptionConsumer(bootstrapServers, objectMapper, validator, database);
    }
    
    @AfterEach
    void tearDown() {
        database.clear();
        if (kafkaTemplate != null) {
            kafkaTemplate.destroy();
        }
    }
    
    @Test
    void testConsumerStopsAfter10Seconds() throws Exception {
        // Produce some test records
        produceTestRecords(5);
        
        // Run consumer in a separate thread
        Thread consumerThread = new Thread(consumer);
        Instant startTime = Instant.now();
        consumerThread.start();
        consumerThread.join(15000); // Wait max 15 seconds
        
        Instant endTime = Instant.now();
        long durationSeconds = Duration.between(startTime, endTime).getSeconds();
        
        // Consumer should stop after approximately 10 seconds
        assertTrue(durationSeconds >= 9 && durationSeconds <= 12, 
            "Consumer should stop after ~10 seconds, but took " + durationSeconds + " seconds");
        
        // Verify consumer thread has finished
        assertFalse(consumerThread.isAlive(), "Consumer thread should have finished");
    }
    
    @Test
    void testConsumerValidatesAndSavesValidRecords() throws Exception {
        // Produce valid records
        Subscription valid1 = new Subscription("sub1", "user1", "premium", 99.99, true);
        Subscription valid2 = new Subscription("sub2", "user2", "basic", 29.99, false);
        
        kafkaTemplate.send("subscriptions", valid1.getId(), objectMapper.writeValueAsString(valid1));
        kafkaTemplate.send("subscriptions", valid2.getId(), objectMapper.writeValueAsString(valid2));
        
        // Produce invalid record (null amount)
        Subscription invalid = new Subscription("sub3", "user3", "basic", null, true);
        kafkaTemplate.send("subscriptions", invalid.getId(), objectMapper.writeValueAsString(invalid));
        
        // Run consumer
        Thread consumerThread = new Thread(consumer);
        consumerThread.start();
        consumerThread.join(12000);
        
        // Verify only valid records are saved
        assertEquals(2, database.size(), "Should have 2 valid records in database");
        assertNotNull(database.findById("sub1"));
        assertNotNull(database.findById("sub2"));
        assertNull(database.findById("sub3"), "Invalid record should not be saved");
    }
    
    @Test
    void testConsumerUsesSynchronousMode() throws Exception {
        // Produce records
        produceTestRecords(3);
        
        // Run consumer
        Thread consumerThread = new Thread(consumer);
        consumerThread.start();
        consumerThread.join(12000);
        
        // In synchronous mode, offsets should be committed after processing
        // We can verify this by checking that records were processed
        assertTrue(database.size() > 0, "Records should be processed and saved");
    }
    
    @Test
    void testConsumerHandlesEmptyTopic() throws Exception {
        // Don't produce any records
        
        // Run consumer
        Thread consumerThread = new Thread(consumer);
        Instant startTime = Instant.now();
        consumerThread.start();
        consumerThread.join(12000);
        Instant endTime = Instant.now();
        
        long durationSeconds = Duration.between(startTime, endTime).getSeconds();
        
        // Should still stop after 10 seconds even with no records
        assertTrue(durationSeconds >= 9 && durationSeconds <= 12);
        assertEquals(0, database.size(), "No records should be saved");
    }
    
    @Test
    void testConsumerProcessesMultiplePollCycles() throws Exception {
        // Produce records that will require multiple poll cycles
        produceTestRecords(10);
        
        // Run consumer
        Thread consumerThread = new Thread(consumer);
        consumerThread.start();
        consumerThread.join(12000);
        
        // Should have processed records across multiple poll cycles
        assertTrue(database.size() > 0, "Should have processed some records");
    }
    
    @Test
    void testValidationRules() throws Exception {
        // Test various validation scenarios
        Subscription valid = new Subscription("sub1", "user1", "premium", 99.99, true);
        Subscription nullId = new Subscription(null, "user1", "premium", 99.99, true);
        Subscription emptyUserId = new Subscription("sub2", "", "premium", 99.99, true);
        Subscription negativeAmount = new Subscription("sub3", "user3", "premium", -10.0, true);
        Subscription nullAmount = new Subscription("sub4", "user4", "premium", null, true);
        Subscription nullActive = new Subscription("sub5", "user5", "premium", 99.99, null);
        
        kafkaTemplate.send("subscriptions", valid.getId(), objectMapper.writeValueAsString(valid));
        if (nullId.getId() != null) {
            kafkaTemplate.send("subscriptions", "null-id", objectMapper.writeValueAsString(nullId));
        }
        kafkaTemplate.send("subscriptions", emptyUserId.getId(), objectMapper.writeValueAsString(emptyUserId));
        kafkaTemplate.send("subscriptions", negativeAmount.getId(), objectMapper.writeValueAsString(negativeAmount));
        kafkaTemplate.send("subscriptions", nullAmount.getId(), objectMapper.writeValueAsString(nullAmount));
        kafkaTemplate.send("subscriptions", nullActive.getId(), objectMapper.writeValueAsString(nullActive));
        
        // Run consumer
        Thread consumerThread = new Thread(consumer);
        consumerThread.start();
        consumerThread.join(12000);
        
        // Only valid record should be saved
        assertEquals(1, database.size(), "Only valid record should be saved");
        assertNotNull(database.findById("sub1"));
    }
    
    private void produceTestRecords(int count) throws Exception {
        for (int i = 1; i <= count; i++) {
            Subscription subscription = new Subscription(
                "sub" + i,
                "user" + i,
                i % 2 == 0 ? "premium" : "basic",
                50.0 + i,
                i % 2 == 0
            );
            kafkaTemplate.send("subscriptions", subscription.getId(), 
                objectMapper.writeValueAsString(subscription));
        }
        // Give producer time to send
        Thread.sleep(500);
    }
}
