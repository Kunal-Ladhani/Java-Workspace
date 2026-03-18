# StampedLock - Complete Guide for SDE2

## Table of Contents

1. [What is StampedLock?](#1-what-is-stampedlock)
2. [Why Was StampedLock Introduced?](#2-why-was-stampedlock-introduced)
3. [The Three Modes](#3-the-three-modes)
4. [How Stamps Work](#4-how-stamps-work)
5. [Optimistic Reading — The Killer Feature](#5-optimistic-reading--the-killer-feature)
6. [Lock Conversion](#6-lock-conversion)
7. [StampedLock vs ReentrantReadWriteLock](#7-stampedlock-vs-reentrantreadwritelock)
8. [Performance Characteristics](#8-performance-characteristics)
9. [Pitfalls and Gotchas](#9-pitfalls-and-gotchas)
10. [When to Use StampedLock](#10-when-to-use-stampedlock)
11. [Best Practices](#11-best-practices)
12. [Interview Questions & Answers](#12-interview-questions--answers)

---

## 1. What is StampedLock?

`StampedLock` is a lock class introduced in **Java 8** (`java.util.concurrent.locks.StampedLock`)
that provides three modes of access control: **writing**, **reading**, and **optimistic reading**.

```java
import java.util.concurrent.locks.StampedLock;

StampedLock sl = new StampedLock();
```

Unlike `ReentrantReadWriteLock`, StampedLock is **not reentrant** and does not implement the
`Lock` or `ReadWriteLock` interfaces. It is designed as a **high-performance alternative**
for read-heavy workloads where the overhead of traditional read locks is too expensive.

The key innovation: **optimistic reads** — a way to read shared data **without acquiring any
lock at all**, then validate that no write occurred during the read.

---

## 2. Why Was StampedLock Introduced?

`ReentrantReadWriteLock` has two performance problems:

### Problem 1: Read lock overhead

Even though multiple readers can hold the lock simultaneously, each reader must still:
1. Perform a CAS operation to increment the shared read count
2. Update a `ThreadLocal` hold counter
3. Perform another CAS on release

Under heavy read contention (many cores), the CAS on the shared read count becomes a
**bottleneck** — all cores compete to update the same cache line.

### Problem 2: Writer starvation

In non-fair mode, continuous readers can starve writers. In fair mode, throughput drops
because readers must queue behind writers.

### StampedLock's solution

- **Optimistic reads require no CAS** — just a volatile read of the stamp. This eliminates
  the shared-counter bottleneck entirely.
- **Lock conversion** provides a safe path from optimistic read → read lock → write lock,
  reducing the upgrade deadlock problem.
- Internal implementation uses a simpler state machine (no ThreadLocal overhead for readers).

---

## 3. The Three Modes

### Mode 1: Write Lock (Exclusive)

```java
StampedLock sl = new StampedLock();
long stamp = sl.writeLock();   // blocks until acquired, returns a stamp
try {
    // exclusive access — no readers or writers
} finally {
    sl.unlockWrite(stamp);      // must pass the stamp from writeLock()
}
```

- Only one thread can hold the write lock
- Blocks all readers and other writers
- Returns a `long` stamp that must be used to unlock
- Semantically identical to any exclusive lock

### Mode 2: Read Lock (Shared / Pessimistic)

```java
long stamp = sl.readLock();     // blocks until no writer active, returns a stamp
try {
    // shared access — multiple readers allowed
} finally {
    sl.unlockRead(stamp);       // must pass the stamp from readLock()
}
```

- Multiple threads can hold the read lock simultaneously
- Blocks if a write lock is held
- Returns a `long` stamp for unlocking
- Similar to `ReentrantReadWriteLock.readLock()`, but **NOT reentrant**

### Mode 3: Optimistic Read (Lock-Free!)

```java
long stamp = sl.tryOptimisticRead();  // returns a stamp immediately — no blocking!
// Read shared fields into local variables
double localX = this.x;
double localY = this.y;
// Validate: did any write happen since we got the stamp?
if (sl.validate(stamp)) {
    // Success! localX and localY are consistent — use them
} else {
    // A writer intervened — fall back to pessimistic read lock
    stamp = sl.readLock();
    try {
        localX = this.x;
        localY = this.y;
    } finally {
        sl.unlockRead(stamp);
    }
}
```

- **No lock is actually acquired** — this is the breakthrough
- `tryOptimisticRead()` returns a stamp (or 0 if write-locked)
- After reading data, call `validate(stamp)` to check if a write occurred
- If valid → the read was consistent, no lock was ever held
- If invalid → fall back to a pessimistic read lock
- **Zero contention on the lock's state** for successful optimistic reads

---

## 4. How Stamps Work

Every lock acquisition returns a `long` value called a **stamp**. This stamp serves as:

1. **A token for unlocking** — you must pass the correct stamp to the unlock method
2. **A version number for validation** — optimistic reads validate against the stamp
3. **A mode indicator** — the stamp encodes which mode the lock was acquired in

### Stamp semantics

```java
long ws = sl.writeLock();            // stamp != 0, encodes write mode
long rs = sl.readLock();             // stamp != 0, encodes read mode
long os = sl.tryOptimisticRead();    // stamp != 0 if no write lock held; 0 if write-locked

sl.validate(os);   // true if no write has occurred since os was obtained
sl.unlockWrite(ws); // releases write lock — stamp must match
sl.unlockRead(rs);  // releases read lock — stamp must match
```

### What happens inside

The stamp is derived from the lock's internal state sequence number. Every write lock
acquisition increments this sequence number. `validate()` checks that the current sequence
number matches the stamp — if it doesn't, a write must have occurred.

```
stamp = state at time of tryOptimisticRead()
         ↓
         ┌─────────────────────────────────────┐
         │  Read local copies of shared data   │
         └─────────────────────────────────────┘
         ↓
validate(stamp) → compare stamp with current state
         ↓
  ┌──────┴──────┐
  │ stamp ==    │ stamp !=
  │ current     │ current
  │             │
  ✅ Valid      ❌ Invalid
  (no write)    (write occurred)
```

### Invalid stamps

- `0` is never a valid stamp
- Using the wrong stamp to unlock throws `IllegalMonitorStateException`
- Stamps from one `StampedLock` instance cannot be used with another

---

## 5. Optimistic Reading — The Killer Feature

Optimistic reading is what sets `StampedLock` apart from all other Java locks. It provides
a way to read shared data with **zero lock overhead** in the common case (no concurrent write).

### The canonical example: Point class

This is the example from the `StampedLock` Javadoc. It perfectly illustrates the pattern:

```java
class Point {
    private double x, y;
    private final StampedLock sl = new StampedLock();

    void move(double deltaX, double deltaY) {
        long stamp = sl.writeLock();
        try {
            x += deltaX;
            y += deltaY;
        } finally {
            sl.unlockWrite(stamp);
        }
    }

    double distanceFromOrigin() {
        // Step 1: Try optimistic read (no lock acquired!)
        long stamp = sl.tryOptimisticRead();
        double currentX = x;
        double currentY = y;

        // Step 2: Validate — did a write happen?
        if (!sl.validate(stamp)) {
            // Step 3: Fall back to pessimistic read lock
            stamp = sl.readLock();
            try {
                currentX = x;
                currentY = y;
            } finally {
                sl.unlockRead(stamp);
            }
        }

        // Step 4: Use the local copies
        return Math.sqrt(currentX * currentX + currentY * currentY);
    }
}
```

### Why is this faster?

1. **No CAS**: `tryOptimisticRead()` is just a volatile read — no atomic compare-and-swap
2. **No cache-line bouncing**: Multiple cores can optimistically read without contending on
   the lock's state variable
3. **No ThreadLocal overhead**: Unlike RRWL's read lock, no per-thread hold counter
4. **No unlock needed**: If validation succeeds, there's nothing to release

### When optimistic reads succeed vs fail

| Scenario                  | `validate()` returns | Action                     |
|---------------------------|----------------------|----------------------------|
| No writes during read     | `true`               | Use the data — done!       |
| Write during read         | `false`              | Fall back to read lock     |
| Write lock held at start  | `stamp == 0`         | Fall back immediately      |

### Critical rule: read into local variables FIRST

```java
// CORRECT ✅ — read into locals, then validate
long stamp = sl.tryOptimisticRead();
double localX = this.x;
double localY = this.y;
if (sl.validate(stamp)) {
    return Math.sqrt(localX * localX + localY * localY);
}

// WRONG ❌ — using fields directly after validate
long stamp = sl.tryOptimisticRead();
if (sl.validate(stamp)) {
    // A write could happen RIGHT HERE, between validate and field access!
    return Math.sqrt(this.x * this.x + this.y * this.y);
}
```

You must copy shared state into local variables **before** calling `validate()`. The
validation only guarantees consistency at the moment `validate()` returns `true` — a write
can happen immediately after.

---

## 6. Lock Conversion

StampedLock supports converting between lock modes without releasing and re-acquiring.
This avoids the "gap" where another thread could intervene.

### `tryConvertToWriteLock(long stamp)`

Attempts to upgrade the current lock to a write lock:

```java
long stamp = sl.readLock();
try {
    // ... discovered we need to write ...
    long ws = sl.tryConvertToWriteLock(stamp);
    if (ws != 0L) {
        stamp = ws;  // conversion succeeded — now holding write lock
        // ... perform write ...
    } else {
        // Conversion failed — must do it the hard way
        sl.unlockRead(stamp);
        stamp = sl.writeLock();  // blocking acquire
    }
    // ... perform write ...
} finally {
    sl.unlock(stamp);  // generic unlock works for any mode
}
```

The conversion succeeds if:
- The write lock is free and no other readers hold the lock, OR
- The current thread is the only reader

### `tryConvertToReadLock(long stamp)`

Downgrade from write to read lock:

```java
long stamp = sl.writeLock();
try {
    // ... perform write ...
    stamp = sl.tryConvertToReadLock(stamp);
    // Now holding read lock — other readers can join
} finally {
    sl.unlock(stamp);
}
```

This always succeeds when holding the write lock (similar to RRWL lock downgrading).

### Conversion from optimistic read

```java
long stamp = sl.tryOptimisticRead();
// ... read data ...
if (!sl.validate(stamp)) {
    // Try converting to read lock (avoids re-reading if possible)
    long rs = sl.tryConvertToReadLock(stamp);
    if (rs != 0L) {
        stamp = rs;
    } else {
        stamp = sl.readLock(); // fallback: blocking read lock
    }
    // re-read data under read lock
}
```

### Conversion matrix

| From \ To          | Optimistic | Read Lock | Write Lock |
|---------------------|------------|-----------|------------|
| Optimistic Read     | N/A        | ✅ `tryConvertToReadLock`  | ✅ `tryConvertToWriteLock` |
| Read Lock           | N/A        | N/A       | ✅ `tryConvertToWriteLock` (if sole reader) |
| Write Lock          | N/A        | ✅ `tryConvertToReadLock`  | N/A        |

All conversions return 0 on failure (non-blocking, never deadlocks).

---

## 7. StampedLock vs ReentrantReadWriteLock

| Feature                       | ReentrantReadWriteLock       | StampedLock                    |
|-------------------------------|------------------------------|--------------------------------|
| **Reentrant**                 | ✅ Yes                       | ❌ No — deadlocks if re-entered |
| **Optimistic read**           | ❌ No                        | ✅ Yes — the major advantage    |
| **Lock conversion (upgrade)** | ❌ No (deadlocks)            | ✅ `tryConvertToWriteLock()`    |
| **Lock downgrade**            | ✅ Yes                       | ✅ `tryConvertToReadLock()`     |
| **Conditions**                | ✅ Write lock supports       | ❌ No condition support         |
| **Fairness policy**           | ✅ Fair / non-fair           | ❌ No fairness guarantee        |
| **Implements Lock/RWL**       | ✅ Yes                       | ❌ No (different API)           |
| **Thread ownership**          | ✅ Tracks owner thread       | ❌ Does not track owners        |
| **Performance (read-heavy)**  | Good                         | Better (optimistic reads)      |
| **Performance (write-heavy)** | Fair                         | Similar                        |
| **Ease of use**               | Easier (standard Lock API)   | Harder (stamps, no reentrancy) |
| **Max readers/writers**       | 65535 each (16-bit split)    | No practical limit             |
| **try-with-resources**        | ❌ Not recommended (either)  | ❌ Must NOT use (stamps)        |
| **Java version**              | Java 5+                      | Java 8+                        |

### When to choose which

- **Choose ReentrantReadWriteLock when:**
  - You need reentrancy (recursive read/write lock calls)
  - You need Condition variables (`await()`/`signal()`)
  - You need fairness guarantees
  - You need the standard `Lock` interface (interoperability)

- **Choose StampedLock when:**
  - Maximum read performance is critical
  - Reads vastly outnumber writes
  - You don't need reentrancy
  - You're comfortable with the stamp-based API

---

## 8. Performance Characteristics

### Optimistic read: near-zero overhead

When no writes are happening:

```
tryOptimisticRead():  ~5-10 ns  (volatile read)
validate():           ~5-10 ns  (volatile read + compare)
Total:                ~10-20 ns
```

Compare with:
```
readLock() + unlock(): ~40-80 ns  (CAS acquire + CAS release)
```

This is a **4-8x improvement** per read operation in the uncontended case.

### Under contention

| Scenario                          | RRWL              | StampedLock          |
|-----------------------------------|--------------------|-----------------------|
| 1 writer, 10 readers             | Good               | Better (optimistic)   |
| 0 writers, 100 readers           | CAS bottleneck     | Near-linear scaling   |
| 50 writers, 50 readers           | Similar            | Similar               |
| 1 writer per 1000 reads          | Good               | Excellent             |

### Why optimistic reads scale better

With `ReentrantReadWriteLock.readLock()`:
- Every reader must CAS the shared state → cache-line bouncing across cores
- With N cores, this creates O(N) contention on a single cache line

With `StampedLock.tryOptimisticRead()`:
- Every reader performs a **local volatile read** → no cache-line bouncing
- Contention is O(1) regardless of the number of cores
- Only writes invalidate the cache line

### When optimistic reads fail frequently

If writes are frequent, `validate()` often returns `false`, and the thread must fall back
to a pessimistic read lock. In this case, StampedLock is **slower** than RRWL because:
1. The optimistic read was wasted work
2. The fallback read lock has similar cost to RRWL's read lock
3. Total cost = (wasted optimistic read) + (pessimistic read lock) > (just read lock)

**Rule of thumb**: If more than ~10-20% of optimistic reads fail validation, consider
using `readLock()` directly or switching to RRWL.

---

## 9. Pitfalls and Gotchas

### Pitfall 1: NOT Reentrant — Deadlocks on Re-Entry

```java
long stamp = sl.writeLock();
long stamp2 = sl.writeLock();  // ❌ DEADLOCK — same thread, but StampedLock doesn't track ownership
```

Unlike `ReentrantLock` and `ReentrantReadWriteLock`, `StampedLock` does **not** track which
thread holds the lock. A thread that tries to re-acquire will block waiting for itself.

**Impact**: You cannot use `StampedLock` in recursive code, or in code where a method
that holds the lock calls another method that also acquires the lock.

### Pitfall 2: Must NOT Use try-with-resources

StampedLock does not implement `AutoCloseable`. More importantly, the stamp-based API
doesn't fit the try-with-resources pattern:

```java
// WRONG ❌ — StampedLock is NOT AutoCloseable
try (StampedLock sl = new StampedLock()) { ... }

// WRONG ❌ — even if you wrapped it, the stamp must be passed to unlock
// There's no way to do this with try-with-resources cleanly
```

Always use explicit try-finally:
```java
long stamp = sl.writeLock();
try {
    // ...
} finally {
    sl.unlockWrite(stamp);
}
```

### Pitfall 3: Using the Wrong Stamp

```java
long readStamp = sl.readLock();
sl.unlockWrite(readStamp);  // ❌ IllegalMonitorStateException — wrong unlock method
```

Each unlock method validates that the stamp matches the expected lock mode.

### Pitfall 4: Stamp Overflow

Internally, the stamp is based on a sequence counter that increments on every write lock
acquisition. After approximately 2^56 write locks (~7.2 × 10^16), the counter can
overflow, potentially causing `validate()` to return incorrect results.

In practice this is unlikely (would take centuries at millions of writes per second), but
it's theoretically possible and mentioned in the Javadoc.

### Pitfall 5: Not Serializable

`StampedLock` is not `Serializable`. Serializing and deserializing a `StampedLock` creates
a new lock in the unlocked state.

### Pitfall 6: Optimistic Read Must Copy to Locals First

```java
// WRONG ❌ — field access after validate is racy
long stamp = sl.tryOptimisticRead();
if (sl.validate(stamp)) {
    return this.x + this.y;  // write could happen between validate() and field access!
}

// CORRECT ✅
long stamp = sl.tryOptimisticRead();
double lx = this.x, ly = this.y;
if (sl.validate(stamp)) {
    return lx + ly;
}
```

### Pitfall 7: No Thread Ownership Tracking

`StampedLock` does not associate locks with specific threads. This means:
- Any thread can unlock (if it has the stamp), not just the one that locked
- `isWriteLocked()` tells you if the lock is held, but not by whom
- No `isHeldByCurrentThread()` equivalent

---

## 10. When to Use StampedLock

### Ideal use cases

1. **Point/coordinate classes** — small mutable objects with x,y,z fields read frequently,
   updated rarely
2. **Read-heavy counters/statistics** — many threads read aggregate stats, rare updates
3. **Configuration objects** — read on every request, updated on config reload
4. **Geometric data structures** — bounding boxes, transforms, matrices
5. **Price/quote feeds** — many consumers read the latest price, one producer updates it

### When NOT to use

1. **Recursive/reentrant locking needed** — use `ReentrantReadWriteLock`
2. **Need Condition variables** — use `ReentrantReadWriteLock` or `ReentrantLock`
3. **Write-heavy workloads** — optimistic reads will fail constantly, no benefit
4. **Need fairness guarantees** — `StampedLock` has no fairness mode
5. **Lock held for long durations** — consider higher-level abstractions
6. **Shared across many classes** — the non-reentrant nature makes this fragile

### Decision tree

```
Do you need reentrancy?
  └─ Yes → ReentrantReadWriteLock (or ReentrantLock)
  └─ No → Do reads vastly outnumber writes (>10:1)?
            └─ No → ReentrantLock
            └─ Yes → Is optimistic read pattern acceptable?
                      └─ No → ReentrantReadWriteLock
                      └─ Yes → StampedLock ✅
```

---

## 11. Best Practices

### 1. Always have a fallback for optimistic reads

```java
long stamp = sl.tryOptimisticRead();
double lx = x, ly = y;
if (!sl.validate(stamp)) {
    stamp = sl.readLock();  // ALWAYS have a fallback
    try {
        lx = x;
        ly = y;
    } finally {
        sl.unlockRead(stamp);
    }
}
```

Never assume optimistic reads will succeed.

### 2. Use the generic `unlock()` when the mode may vary

```java
long stamp = sl.readLock();
try {
    // ... might convert to write lock ...
    long ws = sl.tryConvertToWriteLock(stamp);
    if (ws != 0L) {
        stamp = ws;
        // ... write ...
    }
} finally {
    sl.unlock(stamp);  // works for both read and write stamps
}
```

### 3. Keep optimistic read sections short

The longer the optimistic read section, the higher the chance a write invalidates it.
Copy the minimum data needed into locals and validate immediately.

### 4. Document lock protocol

Because StampedLock's API is less intuitive than `synchronized` or `ReentrantLock`,
document which fields are protected and what the lock protocol is:

```java
/**
 * Thread-safe point. Protected by StampedLock.
 * Write: move() acquires write lock.
 * Read: distanceFromOrigin() uses optimistic read with fallback.
 */
```

### 5. Avoid sharing stamps across methods

The stamp is a method-local value. Don't store it in a field or pass it around carelessly.
This makes the lock protocol harder to reason about and risks using stale stamps.

### 6. Avoid StampedLock in try-with-resources or lambda-heavy code

The stamp-based API doesn't compose well with functional patterns. Keep the lock/unlock
logic explicit and visible.

---

## 12. Interview Questions & Answers

### Q1: What is StampedLock and how is it different from ReentrantReadWriteLock?

**A:** `StampedLock` (Java 8+) is a lock with three modes: write, read, and optimistic read.
The key difference is the **optimistic read mode** — it allows reading shared data without
actually acquiring a lock, then validating that no write occurred. This eliminates CAS
overhead for reads, making it significantly faster under read-heavy workloads. However,
unlike RRWL, StampedLock is NOT reentrant, does NOT support Conditions, and has no fairness
mode.

### Q2: Explain the optimistic read pattern step by step.

**A:** (1) Call `tryOptimisticRead()` to get a stamp — no lock is acquired. (2) Copy shared
fields into local variables. (3) Call `validate(stamp)` — returns true if no write occurred
since the stamp was obtained. (4) If valid, use the local copies. (5) If invalid, fall back
to `readLock()`, re-read the data under the lock, then `unlockRead()`. The critical rule:
always copy to locals before validating, because a write can happen between `validate()` and
any subsequent field access.

### Q3: Why is StampedLock not reentrant? What happens if you re-enter?

**A:** StampedLock does not track which thread holds the lock — it only tracks the lock state
via a sequence counter. If a thread holding a write lock calls `writeLock()` again, it blocks
waiting for the write lock to be released, but it's the one holding it → **deadlock**. This
was a deliberate design choice: tracking thread ownership adds overhead that would negate the
performance benefits, especially for the lightweight optimistic read path.

### Q4: What is lock conversion in StampedLock?

**A:** Lock conversion allows changing the lock mode without releasing and re-acquiring.
`tryConvertToWriteLock(stamp)` attempts to upgrade (optimistic/read → write); it returns a
new write stamp on success or 0 on failure. `tryConvertToReadLock(stamp)` downgrades (write
→ read); this always succeeds when holding the write lock. Conversion is non-blocking — if
it fails, you must fall back to releasing and re-acquiring. This solves the lock upgrade
deadlock problem that plagues RRWL.

### Q5: Why can't you use StampedLock with try-with-resources?

**A:** Two reasons: (1) `StampedLock` doesn't implement `AutoCloseable`. (2) The stamp-based
API requires passing the stamp to the unlock method, which doesn't fit the try-with-resources
pattern. The stamp may also change during lock conversions, so you need mutable local state
to track the current stamp — this is fundamentally incompatible with the resource management
pattern.

### Q6: When would optimistic reads perform WORSE than regular read locks?

**A:** When writes are frequent enough that `validate()` fails often (>10-20% failure rate).
Each failed optimistic read wastes the time spent reading + validating, then requires a
full pessimistic read lock anyway. The total cost = (wasted optimistic attempt) + (fallback
read lock) > (just using read lock from the start). Also, if the read operation is very
short (e.g., reading a single field), the `tryOptimisticRead()` + `validate()` overhead
may exceed the savings.

### Q7: How does the stamp mechanism work internally?

**A:** The stamp is derived from an internal state/sequence counter. Every write lock
acquisition increments this counter. `tryOptimisticRead()` snapshots the current counter
value. `validate()` compares the snapshot against the current value — if they differ, a
write occurred. This is essentially **optimistic concurrency control** — the same concept
used in databases (version columns). The stamp also encodes the lock mode (read/write)
in its lower bits for correct unlock dispatch.

### Q8: Can any thread unlock a StampedLock, or only the thread that locked it?

**A:** Any thread that has the correct stamp can unlock. `StampedLock` does not enforce
thread ownership — it validates only the stamp, not the calling thread's identity. This
is different from `ReentrantLock` and `synchronized`, which are thread-bound. While this
enables patterns like asynchronous lock-then-unlock across callbacks, it also means
accidental unlocking by the wrong thread is possible if stamps are shared.

### Q9: Compare the three read strategies: synchronized, ReadWriteLock read, and StampedLock optimistic read.

**A:**
- `synchronized` / `ReentrantLock`: Exclusive access for reads — simple, but no concurrency
  among readers. Cost: ~20-60 ns.
- `ReentrantReadWriteLock.readLock()`: Shared read lock — allows concurrent readers, but
  each reader must CAS the shared state. Cost: ~40-80 ns. Scales poorly beyond ~16 cores
  due to cache-line contention on the shared read counter.
- `StampedLock.tryOptimisticRead()`: No lock acquired — just a volatile read and validation.
  Cost: ~10-20 ns. Scales linearly with cores because there's no shared state modification.
  Trade-off: requires fallback logic and isn't reentrant.

### Q10: Design a thread-safe 2D Point class. Which lock would you use and why?

**A:** A `StampedLock` is ideal because: (1) Point is typically read-heavy (many `getX()`,
`getY()`, `distanceFrom()` calls vs. rare `move()` calls). (2) Reads are short (just copying
two doubles). (3) No reentrancy needed. (4) Optimistic reads work perfectly — copy x,y to
locals, validate, use. Implementation: `move()` uses `writeLock()`; `distanceFromOrigin()`
uses the optimistic read pattern with fallback to `readLock()`. This is the canonical example
from the StampedLock Javadoc itself.

---

## Quick Reference Cheat Sheet

```java
StampedLock sl = new StampedLock();

// Write lock
long stamp = sl.writeLock();
try { /* exclusive */ } finally { sl.unlockWrite(stamp); }

// Read lock (pessimistic)
long stamp = sl.readLock();
try { /* shared */ } finally { sl.unlockRead(stamp); }

// Optimistic read (the killer feature)
long stamp = sl.tryOptimisticRead();
double lx = x, ly = y;                   // copy to locals
if (!sl.validate(stamp)) {               // check for writes
    stamp = sl.readLock();                // fallback
    try { lx = x; ly = y; } finally { sl.unlockRead(stamp); }
}
// use lx, ly

// Try locks (non-blocking)
long stamp = sl.tryWriteLock();           // 0 if unavailable
long stamp = sl.tryReadLock();            // 0 if unavailable
long stamp = sl.tryWriteLock(1, TimeUnit.SECONDS);  // timed

// Lock conversion
long ws = sl.tryConvertToWriteLock(stamp);  // 0 on failure
long rs = sl.tryConvertToReadLock(stamp);   // 0 on failure

// Generic unlock (works for any mode)
sl.unlock(stamp);

// Query methods
sl.isWriteLocked();           // is write lock held?
sl.isReadLocked();            // is read lock held?
sl.getReadLockCount();        // number of read locks held
```
