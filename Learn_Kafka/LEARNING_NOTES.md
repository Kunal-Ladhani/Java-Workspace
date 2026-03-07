# Learning Notes: Kafka Consumer Timing Control

## The Problem You Encountered

In your OA, you had to implement a consumer that:
- Polls for **3 seconds** per iteration
- Stops consuming after **10 seconds total**
- Uses **synchronous mode**

**What went wrong:**
- Consumer consumed all records in milliseconds
- Consumer kept polling indefinitely
- All test cases failed

## Root Cause

The most common mistake is **not tracking elapsed time** and **not breaking out of the polling loop** when the time limit is reached.

### Wrong Approach (What You Probably Did)
```java
public void run() {
    while (true) {
        ConsumerRecords records = consumer.poll(Duration.ofSeconds(3));
        // Process records...
        consumer.commitSync();
        // ❌ No check to stop after 10 seconds!
    }
}
```

**Problem**: The loop runs forever, only respecting the 3-second poll duration but never stopping after 10 seconds total.

## Correct Solution

### Key Pattern: Track Elapsed Time

```java
public void run() {
    Instant startTime = Instant.now(); // ✅ Track start time
    
    while (true) {
        // ✅ Check elapsed time BEFORE polling
        Instant currentTime = Instant.now();
        long elapsedSeconds = Duration.between(startTime, currentTime).getSeconds();
        
        if (elapsedSeconds >= 10) { // ✅ Stop after 10 seconds
            break;
        }
        
        // ✅ Calculate remaining time for this poll
        long remainingSeconds = 10 - elapsedSeconds;
        long pollDurationMs = Math.min(3000, remainingSeconds * 1000);
        
        // ✅ Poll with calculated duration
        ConsumerRecords records = consumer.poll(Duration.ofMillis(pollDurationMs));
        
        // Process records...
        
        // ✅ Commit synchronously
        consumer.commitSync();
        
        // ✅ Check time AGAIN after processing (processing takes time!)
        currentTime = Instant.now();
        elapsedSeconds = Duration.between(startTime, currentTime).getSeconds();
        if (elapsedSeconds >= 10) {
            break;
        }
    }
}
```

## Critical Points

### 1. Time Tracking
- Use `Instant.now()` to capture start time
- Use `Duration.between()` to calculate elapsed time
- Check elapsed time **before** each poll

### 2. Remaining Time Calculation
- Don't always poll for 3 seconds
- On the last iteration, poll for remaining time only
- Example: If 8 seconds elapsed, poll for only 2 seconds (not 3)

### 3. Time Check After Processing
- Processing records takes time!
- Check elapsed time again after processing
- This prevents exceeding the 10-second limit

### 4. Synchronous Mode
```java
// In consumer properties:
props.put("enable.auto.commit", "false");

// After processing:
consumer.commitSync(); // Manual synchronous commit
```

## Why Your Tests Failed

1. **Time limit exceeded**: Consumer didn't stop after 10 seconds
2. **Records consumed too fast**: No time control, consumed everything immediately
3. **Kept polling**: No break condition, loop never ended

## The Fix in This Project

See `SubscriptionConsumer.java` for the complete implementation:

```java
@Override
public void run() {
    Instant startTime = Instant.now();
    
    while (true) {
        // Check elapsed time
        Instant currentTime = Instant.now();
        long elapsedSeconds = Duration.between(startTime, currentTime).getSeconds();
        
        if (elapsedSeconds >= TOTAL_DURATION_SECONDS) {
            break; // ✅ Stop after 10 seconds
        }
        
        // Calculate poll duration
        long remainingSeconds = TOTAL_DURATION_SECONDS - elapsedSeconds;
        long pollDurationMs = Math.min(POLL_DURATION_SECONDS * 1000, remainingSeconds * 1000);
        
        // Poll
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(pollDurationMs));
        
        // Process records...
        
        // Commit synchronously
        consumer.commitSync();
        
        // Check time again
        currentTime = Instant.now();
        elapsedSeconds = Duration.between(startTime, currentTime).getSeconds();
        if (elapsedSeconds >= TOTAL_DURATION_SECONDS) {
            break;
        }
    }
}
```

## Interview Checklist

When implementing a timed consumer:

- [ ] Track start time with `Instant.now()`
- [ ] Check elapsed time before each poll
- [ ] Calculate remaining time for poll duration
- [ ] Use `Math.min()` to cap poll duration
- [ ] Set `enable.auto.commit=false` for sync mode
- [ ] Call `commitSync()` after processing
- [ ] Check elapsed time again after processing
- [ ] Break out of loop when time limit reached

## Practice Exercise

Try modifying the consumer to:
1. Poll for 2 seconds per iteration
2. Stop after 15 seconds total
3. Add logging to show elapsed time at each poll

This will help you internalize the pattern!
