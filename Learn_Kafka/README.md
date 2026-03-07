# Kafka Learning Project

This project demonstrates how to implement a Kafka consumer with specific timing requirements, which is a common interview/assessment scenario.

## Key Learning Points

### The Problem You Faced

In your OA, you needed to implement a consumer that:
1. **Polls for 3 seconds per iteration**
2. **Stops consuming after 10 seconds total**
3. **Uses synchronous mode** (manual commit)
4. **Validates records** and saves valid ones to database

The issue you encountered: The consumer consumed all records too quickly and kept polling indefinitely.

### The Solution

The key is to **track elapsed time** and **stop the polling loop** when 10 seconds have passed. Here's what the `SubscriptionConsumer.run()` method does:

#### 1. Time Tracking
```java
Instant startTime = Instant.now();
// In the loop:
long elapsedSeconds = Duration.between(startTime, currentTime).getSeconds();
if (elapsedSeconds >= TOTAL_DURATION_SECONDS) {
    break; // Stop consuming
}
```

#### 2. Poll Duration Control
```java
// Poll for 3 seconds OR remaining time, whichever is less
long remainingSeconds = TOTAL_DURATION_SECONDS - elapsedSeconds;
long pollDurationMs = Math.min(POLL_DURATION_SECONDS * 1000, remainingSeconds * 1000);
ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(pollDurationMs));
```

#### 3. Synchronous Mode
```java
props.put("enable.auto.commit", "false"); // Disable auto-commit
// After processing records:
consumer.commitSync(); // Manual synchronous commit
```

#### 4. Validation and Database Save
```java
if (validator.validate(subscription)) {
    database.save(subscription);
}
```

## Project Structure

```
src/main/java/com/learn/kafka/
├── model/
│   └── Subscription.java          # Entity class (ID used as message key)
├── producer/
│   └── SubscriptionProducer.java  # Producer (complete - as given in OA)
├── consumer/
│   └── SubscriptionConsumer.java  # Consumer with proper timing control
├── service/
│   ├── SubscriptionValidator.java # Validation logic
│   └── InMemoryDatabase.java     # In-memory DB using HashMap
├── config/
│   └── KafkaConfig.java           # Kafka configuration
└── runner/
    └── KafkaDemoRunner.java       # Demo runner

src/test/java/com/learn/kafka/
└── consumer/
    └── SubscriptionConsumerTest.java  # Comprehensive test cases
```

## Key Concepts Explained

### 1. Poll Duration vs Total Duration

- **Poll Duration (3s)**: How long each `consumer.poll()` call waits for records
- **Total Duration (10s)**: Maximum time the consumer should run overall

**Common Mistake**: Not tracking total elapsed time, causing infinite polling.

**Solution**: Track start time and check elapsed time before each poll.

### 2. Synchronous Mode

- **Auto-commit (async)**: Kafka automatically commits offsets periodically
- **Manual commit (sync)**: You explicitly call `commitSync()` after processing

**Why synchronous?**: Ensures records are fully processed before committing offsets. If consumer crashes, uncommitted records will be reprocessed.

### 3. Time Management in Consumer Loop

The critical pattern:
```java
while (true) {
    // 1. Check if time limit reached
    if (elapsed >= TOTAL_DURATION) break;
    
    // 2. Calculate poll duration (3s or remaining time)
    long pollMs = Math.min(3000, remaining * 1000);
    
    // 3. Poll for records
    ConsumerRecords records = consumer.poll(Duration.ofMillis(pollMs));
    
    // 4. Process records
    // ...
    
    // 5. Commit synchronously
    consumer.commitSync();
    
    // 6. Check time again (records processing might have taken time)
    if (elapsed >= TOTAL_DURATION) break;
}
```

## Running the Project

### Prerequisites
- Java 17+
- Maven
- Kafka running on localhost:9092 (or update `application.yaml`)

### Run Tests
```bash
mvn test
```

### Run Demo
```bash
mvn spring-boot:run
```

The demo will:
1. Produce 5 subscription records (4 valid, 1 invalid)
2. Start the consumer
3. Consumer will poll for 3s per iteration
4. Consumer will stop after 10s total
5. Only valid records will be saved to database

## Test Cases

The test suite (`SubscriptionConsumerTest`) covers:
- ✅ Consumer stops after 10 seconds
- ✅ Valid records are validated and saved
- ✅ Invalid records are rejected
- ✅ Synchronous mode (manual commit)
- ✅ Handles empty topic gracefully
- ✅ Processes multiple poll cycles
- ✅ Validation rules (null checks, positive amounts, etc.)

## Common Pitfalls to Avoid

1. **Not tracking elapsed time**: Consumer runs forever
2. **Not checking time after processing**: Processing takes time, might exceed limit
3. **Using auto-commit when sync mode required**: Offsets committed before processing completes
4. **Not handling empty polls**: Consumer should still respect time limit
5. **Not calculating remaining time**: Last poll might exceed total duration

## Interview Tips

When asked to implement a timed consumer:
1. ✅ Track start time using `Instant.now()`
2. ✅ Check elapsed time before each poll
3. ✅ Calculate remaining time for last poll
4. ✅ Use `Duration.between()` for time calculations
5. ✅ Set `enable.auto.commit=false` for sync mode
6. ✅ Call `commitSync()` after processing
7. ✅ Check time again after processing (it takes time!)

## Next Steps

- Try modifying the poll duration and total duration
- Experiment with different validation rules
- Add more complex processing logic
- Test with larger volumes of records
- Learn about consumer groups and partitions
