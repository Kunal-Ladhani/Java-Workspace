# Semaphore - Complete Guide for SDE2

## Table of Contents

1. [What is a Semaphore?](#1-what-is-a-semaphore)
2. [Historical Context — Dijkstra's P and V Operations](#2-historical-context--dijkstras-p-and-v-operations)
3. [Counting Semaphore vs Binary Semaphore](#3-counting-semaphore-vs-binary-semaphore)
4. [Java's Semaphore Class](#4-javas-semaphore-class)
5. [acquire() and release() — Detailed Semantics](#5-acquire-and-release--detailed-semantics)
6. [Fairness — Fair vs Non-Fair Semaphore](#6-fairness--fair-vs-non-fair-semaphore)
7. [tryAcquire() — Non-Blocking Variants](#7-tryacquire--non-blocking-variants)
8. [Semaphore vs Lock](#8-semaphore-vs-lock)
9. [Semaphore vs Mutex](#9-semaphore-vs-mutex)
10. [Use Cases](#10-use-cases)
11. [Internal Implementation (AQS Shared Mode)](#11-internal-implementation-aqs-shared-mode)
12. [The Parking Lot Analogy](#12-the-parking-lot-analogy)
13. [Common Pitfalls](#13-common-pitfalls)
14. [Interview Questions & Answers](#14-interview-questions--answers)

---

## 1. What is a Semaphore?

A **Semaphore** is a synchronization primitive that controls access to a shared resource through
a set of **permits**. It maintains a counter representing the number of available permits. Threads
must **acquire** a permit before proceeding and **release** it when done.

Unlike a lock or mutex, a semaphore does NOT enforce **ownership**. Any thread can release a
permit — it doesn't have to be the same thread that acquired it.

```
Semaphore(permits=3)

Thread-1 acquires → permits = 2
Thread-2 acquires → permits = 1
Thread-3 acquires → permits = 0
Thread-4 acquires → BLOCKS (waits until a permit is released)
Thread-1 releases → permits = 1 → Thread-4 unblocks
```

Think of it as a **bouncer at a club**: the club has a maximum capacity (permits). When full,
new guests must wait until someone leaves.

---

## 2. Historical Context — Dijkstra's P and V Operations

Semaphores were invented by **Edsger Dijkstra** in 1965. The original operations were named
from Dutch:

| Operation | Dutch         | English              | Java Equivalent |
|-----------|---------------|----------------------|-----------------|
| **P**     | Proberen      | "to test" / "try"    | `acquire()`     |
| **V**     | Verhogen      | "to increment"       | `release()`     |

The P operation (proberen) **decrements** the semaphore value. If the value becomes negative,
the thread is blocked. The V operation (verhogen) **increments** the value. If there are
blocked threads, one is unblocked.

Dijkstra's original definition used an integer that could go negative (the absolute value
representing the number of waiting threads). Java's implementation keeps the count at zero
minimum — waiting threads are tracked in a queue instead.

```
Dijkstra's model:
  P(S): S = S - 1; if S < 0, block
  V(S): S = S + 1; if S <= 0, unblock one waiting thread

Java's model:
  acquire(): if permits > 0, decrement; else block
  release(): increment permits; signal one waiting thread
```

---

## 3. Counting Semaphore vs Binary Semaphore

### Counting Semaphore

A semaphore initialized with a count **N > 1**. Allows up to N threads to access the
resource concurrently.

```java
Semaphore pool = new Semaphore(5); // 5 concurrent accesses allowed
```

Use case: connection pools, thread pools, rate limiters.

### Binary Semaphore

A semaphore initialized with a count of **1**. Only one thread can hold the permit at a time.

```java
Semaphore binarySem = new Semaphore(1); // acts like a lock (sort of)
```

**Critical distinction**: A binary semaphore is NOT the same as a mutex. See Section 9.

### Comparison Table

| Property              | Counting Semaphore   | Binary Semaphore     |
|-----------------------|----------------------|----------------------|
| Permit count          | N (any positive int) | 1                    |
| Concurrent access     | Up to N threads      | 1 thread             |
| Ownership             | None                 | None                 |
| Use case              | Resource pooling     | Signaling, simple mutual exclusion |

---

## 4. Java's Semaphore Class

```java
import java.util.concurrent.Semaphore;
```

### Constructors

```java
Semaphore sem = new Semaphore(3);        // 3 permits, non-fair (default)
Semaphore fairSem = new Semaphore(3, true);  // 3 permits, fair (FIFO)
```

### Key Methods

| Method                                   | Description                                                |
|------------------------------------------|------------------------------------------------------------|
| `acquire()`                              | Acquires 1 permit; blocks if none available                |
| `acquire(int n)`                         | Acquires n permits; blocks if not enough available         |
| `acquireUninterruptibly()`               | Like acquire() but ignores interrupts                      |
| `release()`                              | Releases 1 permit                                          |
| `release(int n)`                         | Releases n permits                                         |
| `tryAcquire()`                           | Tries to acquire; returns immediately (true/false)         |
| `tryAcquire(long timeout, TimeUnit)`     | Tries to acquire with timeout                              |
| `availablePermits()`                     | Returns current number of available permits                |
| `drainPermits()`                         | Acquires and returns all immediately available permits     |
| `hasQueuedThreads()`                     | Whether any threads are waiting to acquire                 |
| `getQueueLength()`                       | Estimated number of waiting threads                        |

---

## 5. acquire() and release() — Detailed Semantics

### acquire()

```java
semaphore.acquire(); // blocks until a permit is available
```

1. **Decrements** the internal permit count by 1.
2. If permits > 0 before decrement, the thread proceeds immediately.
3. If permits == 0, the thread is **parked** (put to sleep) and added to a wait queue.
4. The thread remains blocked until another thread calls `release()`.
5. `acquire()` responds to **interrupts** — it throws `InterruptedException` if the
   thread is interrupted while waiting.

### release()

```java
semaphore.release(); // increments permits, potentially unblocks a waiter
```

1. **Increments** the internal permit count by 1.
2. If there are threads waiting in the queue, one of them is **unparked** (woken up).
3. **CRITICAL**: `release()` can be called by **ANY thread** — not just the one that
   called `acquire()`. This is fundamentally different from locks!

```java
// This is perfectly legal with a Semaphore:
// Thread A acquires, Thread B releases
Semaphore sem = new Semaphore(1);

// Thread A:
sem.acquire();

// Thread B (different thread!):
sem.release(); // LEGAL — no ownership check
```

### acquire(int permits)

```java
semaphore.acquire(3); // atomically acquires 3 permits
```

This is an **all-or-nothing** operation. If only 2 permits are available out of 3 requested,
the thread blocks until all 3 are available. It does NOT acquire 2 and wait for 1 more.

### release(int permits)

```java
semaphore.release(3); // releases 3 permits at once
```

**Warning**: You can release more permits than you acquired! This will increase the total
permit count beyond the initial value. Java's Semaphore does NOT track initial capacity.

```java
Semaphore sem = new Semaphore(3);
sem.release(); // permits = 4 now! No error!
sem.release(10); // permits = 14 now! Still no error!
```

---

## 6. Fairness — Fair vs Non-Fair Semaphore

### Non-Fair (Default)

```java
Semaphore sem = new Semaphore(3); // non-fair
```

- No guarantees about the order in which waiting threads acquire permits.
- A thread that just arrived might acquire a permit before a thread that has been waiting longer.
- This is called **barging** — threads can "cut in line."
- **Better throughput** because there's no overhead of maintaining strict FIFO ordering.

### Fair

```java
Semaphore fairSem = new Semaphore(3, true); // fair
```

- Waiting threads acquire permits in **FIFO order** (first-come, first-served).
- Prevents starvation — no thread waits indefinitely while others keep barging.
- **Lower throughput** due to the overhead of maintaining the queue order.

### When to Use Fair

- When **starvation** is a concern (long-running systems where some threads might never get
  permits).
- When **predictable latency** matters more than raw throughput.
- When you need **fairness guarantees** for correctness (rare — most systems prefer throughput).

### Note on tryAcquire() and Fairness

`tryAcquire()` (the no-arg version) **always barges** even on a fair semaphore. If you want
fair behavior with non-blocking semantics, use:

```java
sem.tryAcquire(0, TimeUnit.SECONDS); // respects fairness
```

---

## 7. tryAcquire() — Non-Blocking Variants

### tryAcquire() — Immediate

```java
if (sem.tryAcquire()) {
    try {
        // use the resource
    } finally {
        sem.release();
    }
} else {
    // couldn't acquire — do something else (fallback, return error, etc.)
}
```

- Returns `true` if a permit was acquired, `false` otherwise.
- **Never blocks**. Returns immediately.
- **Barges** regardless of fairness setting.

### tryAcquire(timeout, TimeUnit) — Timed

```java
if (sem.tryAcquire(5, TimeUnit.SECONDS)) {
    try {
        // use the resource
    } finally {
        sem.release();
    }
} else {
    // timed out — resource wasn't available within 5 seconds
}
```

- Waits up to the specified timeout.
- Returns `true` if acquired within the timeout, `false` if timed out.
- **Respects fairness** (unlike the no-arg version).
- Throws `InterruptedException` if interrupted while waiting.

### tryAcquire(int permits) and tryAcquire(int permits, timeout, TimeUnit)

```java
if (sem.tryAcquire(3, 2, TimeUnit.SECONDS)) {
    // acquired 3 permits within 2 seconds
}
```

---

## 8. Semaphore vs Lock

| Property                  | Semaphore                    | Lock (ReentrantLock)          |
|---------------------------|------------------------------|-------------------------------|
| **Ownership**             | No owner                     | Thread that locked owns it    |
| **Who can release**       | Any thread                   | Only the owner thread         |
| **Reentrancy**            | Not reentrant (no ownership) | Reentrant (same thread can lock multiple times) |
| **Permit count**          | N permits (configurable)     | Binary (locked/unlocked)      |
| **Condition variables**   | Not supported                | Supported via `newCondition()` |
| **Purpose**               | Control concurrent access count | Mutual exclusion              |
| **IllegalMonitorState**   | Never thrown on release       | Thrown if non-owner releases  |

The **key insight**: A lock protects a critical section and has an owner. A semaphore
controls access to a resource pool and has no owner. These are fundamentally different
abstractions even though they look similar.

```java
// Lock: only the thread that locked can unlock
ReentrantLock lock = new ReentrantLock();
lock.lock();
// Only THIS thread can call lock.unlock() — other threads get IllegalMonitorStateException

// Semaphore: any thread can release
Semaphore sem = new Semaphore(1);
sem.acquire(); // Thread A
// Thread B can call sem.release() — perfectly legal
```

---

## 9. Semaphore vs Mutex

This is one of the most commonly confused topics in interviews.

### A Binary Semaphore is NOT a Mutex

Even though `Semaphore(1)` allows only one thread to hold the permit at a time (just like a
mutex), it is fundamentally different:

| Property                     | Mutex                        | Binary Semaphore             |
|------------------------------|------------------------------|------------------------------|
| **Ownership**                | YES — only owner can unlock  | NO — any thread can release  |
| **Reentrancy**               | Can be reentrant             | Cannot be reentrant          |
| **Priority Inversion**       | Can use priority inheritance | Cannot (no ownership info)   |
| **Error detection**          | Detects wrong-thread unlock  | No detection                 |
| **Purpose**                  | Mutual exclusion             | Signaling / resource control |

### Why Ownership Matters

1. **Error Detection**: If Thread A acquires a mutex and Thread B accidentally tries to
   release it, the system catches the bug. With a binary semaphore, it silently succeeds
   and breaks your invariant.

2. **Reentrancy**: A mutex knows its owner, so the same thread can lock it again without
   deadlocking. A semaphore has no owner, so `acquire()` on an already-acquired
   `Semaphore(1)` will deadlock.

3. **Priority Inheritance**: The OS can boost the priority of the mutex owner to prevent
   priority inversion. With a semaphore, there's no owner to boost.

```java
// Mutex (ReentrantLock) — reentrant
ReentrantLock mutex = new ReentrantLock();
mutex.lock();
mutex.lock();   // OK — same thread, hold count = 2
mutex.unlock();
mutex.unlock();

// Binary Semaphore — NOT reentrant
Semaphore sem = new Semaphore(1);
sem.acquire();
sem.acquire();  // DEADLOCK! This thread is waiting for itself
```

### When to Use Each

- **Mutual exclusion** → Use `synchronized` or `ReentrantLock` (true mutexes)
- **Resource pooling** (N concurrent accesses) → Use `Semaphore(N)`
- **Thread signaling** (one thread signals another) → Use `Semaphore(0)` as a signal
- **Rate limiting** → Use `Semaphore(maxConcurrent)`

---

## 10. Use Cases

### Use Case 1: Rate Limiting / Throttling

Limit the number of concurrent requests to an API or service.

```java
public class RateLimiter {
    private final Semaphore semaphore;

    public RateLimiter(int maxConcurrent) {
        this.semaphore = new Semaphore(maxConcurrent);
    }

    public String callApi(String request) throws InterruptedException {
        semaphore.acquire();
        try {
            return doApiCall(request);
        } finally {
            semaphore.release();
        }
    }
}
```

### Use Case 2: Connection Pooling

Limit the number of concurrent database connections.

```java
public class ConnectionPool {
    private final Semaphore semaphore;
    private final Queue<Connection> pool;

    public ConnectionPool(int maxConnections) {
        this.semaphore = new Semaphore(maxConnections);
        this.pool = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < maxConnections; i++) {
            pool.add(createConnection());
        }
    }

    public Connection getConnection() throws InterruptedException {
        semaphore.acquire();
        return pool.poll();
    }

    public void returnConnection(Connection conn) {
        pool.offer(conn);
        semaphore.release();
    }
}
```

### Use Case 3: Resource Access Control

Allow at most N threads into a critical section.

```java
Semaphore resourceGuard = new Semaphore(5);

public void accessResource() throws InterruptedException {
    resourceGuard.acquire();
    try {
        // At most 5 threads execute this block concurrently
        useSharedResource();
    } finally {
        resourceGuard.release();
    }
}
```

### Use Case 4: Producer-Consumer Signaling

Using `Semaphore(0)` as a signal: the producer releases, the consumer acquires.

```java
Semaphore itemAvailable = new Semaphore(0);

// Producer:
buffer.add(item);
itemAvailable.release(); // signal: "an item is ready"

// Consumer:
itemAvailable.acquire(); // wait for signal
item = buffer.remove();
```

Note: For actual producer-consumer, prefer `BlockingQueue` — it handles synchronization
internally and is less error-prone.

### Use Case 5: Bounded Resource in Microservices

In a microservice, limit concurrent calls to a downstream service to prevent overwhelming it:

```java
public class CircuitBreaker {
    private final Semaphore concurrencyLimit = new Semaphore(10);

    public Response callDownstream(Request req) {
        if (!concurrencyLimit.tryAcquire()) {
            return Response.serviceUnavailable("Too many concurrent requests");
        }
        try {
            return httpClient.send(req);
        } finally {
            concurrencyLimit.release();
        }
    }
}
```

---

## 11. Internal Implementation (AQS Shared Mode)

Java's `Semaphore` is built on **AbstractQueuedSynchronizer (AQS)**, the same framework
that powers `ReentrantLock`, `CountDownLatch`, and `ReentrantReadWriteLock`.

### How AQS Works for Semaphore

- AQS maintains an `int state` field. For Semaphore, **state = available permits**.
- Semaphore uses AQS in **shared mode** (multiple threads can acquire simultaneously), unlike
  `ReentrantLock` which uses **exclusive mode**.

### acquire() Implementation (Simplified)

```
1. Read current state (permits)
2. If permits > 0:
   a. CAS(state, permits, permits - 1)
   b. If CAS succeeds → return (acquired!)
   c. If CAS fails → retry from step 1
3. If permits == 0:
   a. Create a Node for this thread
   b. Add Node to the CLH wait queue
   c. Park the thread (LockSupport.park)
   d. When unparked → retry from step 1
```

### release() Implementation (Simplified)

```
1. Read current state (permits)
2. CAS(state, permits, permits + 1)
3. If CAS fails → retry from step 1
4. If CAS succeeds:
   a. Check if any threads are in the wait queue
   b. If yes → unpark the head of the queue
```

### Fair vs Non-Fair in AQS

- **Non-fair**: In step 2 of acquire, the thread tries to CAS immediately, even if there
  are threads in the queue (barging).
- **Fair**: In step 2, the thread first checks `hasQueuedPredecessors()`. If there are
  threads ahead of it in the queue, it doesn't try to CAS — it goes to the queue instead.

### Key Classes

```
Semaphore
  └── Sync extends AQS          (inner abstract class)
       ├── NonfairSync           (default — allows barging)
       └── FairSync              (FIFO ordering)
```

---

## 12. The Parking Lot Analogy

This is the best mental model for understanding semaphores:

```
PARKING LOT (Semaphore)
┌─────────────────────────────────────┐
│  Capacity: 3 spaces (permits = 3)   │
│                                     │
│  [Car A]  [Car B]  [  empty  ]      │  permits = 1
│                                     │
└─────────────────────────────────────┘
         │
         │ entrance (acquire)
    ┌────┴────┐
    │ Gate    │◄── If spaces > 0: open gate, decrement counter
    │ (P/V)  │    If spaces = 0: car waits in line
    └────┬────┘
         │ exit (release)
         │
    Cars waiting: [Car C] [Car D] ...  (wait queue)
```

| Parking Lot       | Semaphore                     |
|--------------------|-------------------------------|
| Total spaces       | Initial permit count          |
| Available spaces   | `availablePermits()`          |
| Car enters         | `acquire()`                   |
| Car exits          | `release()`                   |
| Lot full           | Permits = 0, threads block    |
| Waiting line       | AQS wait queue                |
| **Any person** can move a car out | **Any thread** can call release |

The last row is the key insight: unlike a lock (where only the person who parked can
retrieve their car), a semaphore lets **anyone** free up a space.

---

## 13. Common Pitfalls

### Pitfall 1: Forgetting to Release

```java
// BAD — if doWork() throws, permit is leaked
sem.acquire();
doWork();
sem.release();

// GOOD — always use try-finally
sem.acquire();
try {
    doWork();
} finally {
    sem.release();
}
```

If you forget to release, the permit count permanently decreases. Eventually, all permits
are leaked and no thread can ever acquire again — a form of **resource starvation**.

### Pitfall 2: Releasing More Than Acquiring

```java
Semaphore sem = new Semaphore(3);

// Thread mistakenly releases without acquiring
sem.release(); // permits = 4! Exceeds initial capacity!
```

Java's Semaphore does NOT track the initial permit count. Extra releases silently increase
permits beyond the original count. This can break your invariant (e.g., a "3-connection pool"
now allows 4 concurrent connections).

**Prevention**: Always pair acquire/release in try-finally blocks. Consider wrapping the
Semaphore to track the initial count.

### Pitfall 3: Using Binary Semaphore as a Mutex

```java
Semaphore mutex = new Semaphore(1);

// Thread A:
mutex.acquire();
mutex.acquire(); // DEADLOCK — not reentrant!

// Compare with ReentrantLock:
ReentrantLock lock = new ReentrantLock();
lock.lock();
lock.lock(); // Fine — reentrant, hold count = 2
```

### Pitfall 4: Ignoring InterruptedException

```java
// BAD — swallowing the interrupt
try {
    sem.acquire();
} catch (InterruptedException e) {
    // doing nothing loses the interrupt signal
}

// GOOD — restore the interrupt flag
try {
    sem.acquire();
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    throw new RuntimeException("Interrupted while acquiring permit", e);
}
```

### Pitfall 5: Not Using Fair Semaphore When Needed

In long-running systems with high contention, non-fair semaphores can cause **starvation**
where some threads never get permits. If you see threads timing out or getting starved,
switch to `new Semaphore(n, true)`.

### Pitfall 6: Acquire and Release Count Mismatch

```java
sem.acquire(3);    // acquires 3 permits
// ... work ...
sem.release();     // ONLY releases 1! 2 permits leaked!

// Correct:
sem.acquire(3);
try {
    // ... work ...
} finally {
    sem.release(3); // release the same number
}
```

---

## 14. Interview Questions & Answers

### Q1: What is a Semaphore? How does it differ from a lock?

**Answer**: A Semaphore is a synchronization primitive that maintains a set of permits.
Threads acquire permits before accessing a resource and release them afterward. Unlike a
lock, a semaphore has **no ownership** — any thread can release a permit, not just the one
that acquired it. A lock is binary (locked/unlocked) and has an owner; a counting semaphore
allows N concurrent accesses. Semaphores are for controlling concurrency level, while locks
are for mutual exclusion.

---

### Q2: Is a binary semaphore the same as a mutex? Explain.

**Answer**: No. While both allow only one thread at a time, they differ fundamentally:
1. **Ownership**: A mutex tracks which thread locked it; only that thread can unlock.
   A binary semaphore has no ownership — any thread can release.
2. **Reentrancy**: A reentrant mutex allows the same thread to lock multiple times.
   A binary semaphore will deadlock if the same thread acquires twice.
3. **Priority inheritance**: Possible with mutex (OS knows the owner), not with semaphore.
4. **Error detection**: Mutex detects wrong-thread-unlock; semaphore doesn't.

In Java: `ReentrantLock` = mutex, `Semaphore(1)` = binary semaphore.

---

### Q3: Can permits exceed the initial count? Is that a bug?

**Answer**: Yes, permits can exceed the initial count! If you call `release()` without a
prior `acquire()`, permits increase beyond the initial value. Java's `Semaphore` does NOT
enforce an upper bound. This is by design — it enables patterns like signaling
(`Semaphore(0)` where the producer releases without ever acquiring). However, in resource
pooling scenarios, it IS a bug. Always pair acquire/release and use try-finally.

---

### Q4: Explain fair vs non-fair semaphore. When would you choose each?

**Answer**: A **non-fair** semaphore (default) allows barging — a newly arriving thread can
acquire a permit before threads already waiting. This gives **higher throughput** because
there's no overhead of enforcing ordering. A **fair** semaphore grants permits in FIFO
order, preventing starvation but with lower throughput. Choose fair when: starvation is a
risk (long-running servers), predictable latency matters, or the system has high contention
where some threads might never get served.

---

### Q5: How would you implement a connection pool using Semaphore?

**Answer**: Initialize a `Semaphore(N)` where N is the maximum number of connections.
Store connections in a thread-safe collection (e.g., `ConcurrentLinkedQueue`). To borrow
a connection: `semaphore.acquire()` then `queue.poll()`. To return: `queue.offer(conn)`
then `semaphore.release()`. The semaphore ensures at most N connections are in use
simultaneously. Use try-finally to guarantee release even on exceptions. Consider using
`tryAcquire(timeout)` to avoid indefinite blocking.

---

### Q6: What happens internally when acquire() is called and no permits are available?

**Answer**: The thread enters AQS's CLH wait queue. A new `Node` is created for the
thread and appended to the queue's tail via CAS. Then `LockSupport.park()` is called,
which suspends the thread (it won't consume CPU). When another thread calls `release()`,
AQS increments the state (permits) and calls `LockSupport.unpark()` on the head node's
thread. The unparked thread retries the CAS to acquire permits. In a fair semaphore, it
also checks `hasQueuedPredecessors()` to maintain FIFO ordering.

---

### Q7: How is tryAcquire() different from acquire()? When would you use it?

**Answer**: `acquire()` blocks indefinitely until a permit is available.
`tryAcquire()` returns immediately with `true` (acquired) or `false` (not available).
`tryAcquire(timeout, TimeUnit)` waits up to the specified time.

Use `tryAcquire()` when:
- You want to provide a fallback (e.g., return HTTP 503 instead of blocking).
- You're implementing non-blocking algorithms.
- You want to avoid deadlocks by timing out.
- You're building circuit breakers or bulkheads in microservices.

Note: `tryAcquire()` (no-arg) **always barges** even on fair semaphores. Use
`tryAcquire(0, TimeUnit.SECONDS)` if you need fairness.

---

### Q8: Design a rate limiter that allows at most K concurrent operations.

**Answer**:

```java
public class ConcurrencyLimiter<T> {
    private final Semaphore semaphore;

    public ConcurrencyLimiter(int maxConcurrent) {
        this.semaphore = new Semaphore(maxConcurrent, true);
    }

    public T execute(Callable<T> task) throws Exception {
        semaphore.acquire();
        try {
            return task.call();
        } finally {
            semaphore.release();
        }
    }

    public Optional<T> tryExecute(Callable<T> task, long timeout, TimeUnit unit) {
        try {
            if (semaphore.tryAcquire(timeout, unit)) {
                try {
                    return Optional.of(task.call());
                } finally {
                    semaphore.release();
                }
            }
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }
        return Optional.empty();
    }
}
```

---

### Q9: What is the relationship between Semaphore and AQS?

**Answer**: `Semaphore` is implemented on top of `AbstractQueuedSynchronizer` (AQS) using
**shared mode**. AQS's `state` field stores the available permit count. `acquire()` maps
to `acquireSharedInterruptibly()`, which tries to decrement state via CAS. `release()` maps
to `releaseShared()`, which increments state via CAS. AQS provides the CLH queue for parking
blocked threads and the CAS operations for lock-free state management. The fair/non-fair
behavior is determined by the inner `Sync` subclass: `FairSync` checks
`hasQueuedPredecessors()` before allowing acquisition; `NonfairSync` doesn't.

---

### Q10: Can you use a Semaphore for thread signaling? How?

**Answer**: Yes. Initialize `Semaphore(0)`. The signaling pattern:
- **Waiting thread**: calls `acquire()` — blocks because permits = 0.
- **Signaling thread**: calls `release()` — increments permits to 1, unblocking the waiter.

This is essentially a **one-shot signal** (like `CountDownLatch(1)` but reusable). It works
because semaphores have no ownership — the releasing thread doesn't need to have acquired
first. This pattern is used for sequencing operations between threads.

```java
Semaphore signal = new Semaphore(0);

// Thread A (waiter):
signal.acquire(); // blocks until Thread B signals
doWorkAfterSignal();

// Thread B (signaler):
prepareData();
signal.release(); // unblocks Thread A
```
