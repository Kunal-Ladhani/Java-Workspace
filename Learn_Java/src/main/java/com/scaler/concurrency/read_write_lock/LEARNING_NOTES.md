# ReentrantReadWriteLock - Complete Guide for SDE2

## Table of Contents

1. [What is a ReadWriteLock?](#1-what-is-a-readwritelock)
2. [The Problem with Exclusive Locks](#2-the-problem-with-exclusive-locks)
3. [Shared Lock vs Exclusive Lock](#3-shared-lock-read-vs-exclusive-lock-write)
4. [Lock Compatibility Matrix](#4-lock-compatibility-matrix)
5. [ReentrantReadWriteLock — The Concrete Implementation](#5-reentrantreadwritelock--the-concrete-implementation)
6. [Reentrancy in ReadWriteLock](#6-reentrancy-in-readwritelock)
7. [Lock Downgrading and Upgrading](#7-lock-downgrading-and-upgrading)
8. [Fairness: Fair vs Non-Fair Mode](#8-fairness-fair-vs-non-fair-mode)
9. [Writer Starvation Problem](#9-writer-starvation-problem)
10. [Internal Implementation (AQS)](#10-internal-implementation-aqs)
11. [Performance Characteristics](#11-performance-characteristics)
12. [When to Use ReadWriteLock vs ReentrantLock vs synchronized](#12-when-to-use-readwritelock-vs-reentrantlock-vs-synchronized)
13. [Real-World Use Cases](#13-real-world-use-cases)
14. [Best Practices and Pitfalls](#14-best-practices-and-pitfalls)
15. [Interview Questions & Answers](#15-interview-questions--answers)

---

## 1. What is a ReadWriteLock?

`ReadWriteLock` is an interface in `java.util.concurrent.locks` that defines a **pair of locks**:
one for **read-only operations** and one for **write operations**.

```java
public interface ReadWriteLock {
    Lock readLock();
    Lock writeLock();
}
```

The fundamental insight: **multiple threads can safely read shared data simultaneously** as long
as no thread is writing. A traditional exclusive lock (like `ReentrantLock` or `synchronized`)
forces all threads — even readers — to wait in line. This is wasteful when reads vastly
outnumber writes.

A `ReadWriteLock` allows:
- **Multiple concurrent readers** — read lock is a *shared* lock
- **Exclusive writer access** — write lock is an *exclusive* lock

---

## 2. The Problem with Exclusive Locks

Consider a thread-safe cache with 95% reads and 5% writes:

```java
// With ReentrantLock — every access is serialized
private final ReentrantLock lock = new ReentrantLock();
private final Map<String, Object> cache = new HashMap<>();

public Object get(String key) {
    lock.lock();          // Reader blocks other readers! ❌
    try {
        return cache.get(key);
    } finally {
        lock.unlock();
    }
}

public void put(String key, Object value) {
    lock.lock();
    try {
        cache.put(key, value);
    } finally {
        lock.unlock();
    }
}
```

If 100 threads all call `get()` concurrently, they execute **one at a time** even though
reads don't conflict with each other. Under high read contention, this becomes a severe
bottleneck.

### The throughput problem

With an exclusive lock and N reader threads:
- Effective concurrency for reads = **1** (always serial)
- Read throughput = single-threaded throughput

With a ReadWriteLock and N reader threads:
- Effective concurrency for reads = **N** (all can proceed simultaneously)
- Read throughput ≈ N × single-threaded throughput (minus lock overhead)

---

## 3. Shared Lock (Read) vs Exclusive Lock (Write)

### Read Lock (Shared Lock)

- Multiple threads can acquire the **read lock simultaneously**
- Guarantees that no writer is active while readers hold the lock
- Does NOT prevent other readers from entering
- Does NOT modify shared state (by convention — the lock doesn't enforce this)

```java
ReadWriteLock rwLock = new ReentrantReadWriteLock();
Lock readLock = rwLock.readLock();

readLock.lock();
try {
    // Multiple threads can be here concurrently
    return sharedData.get(key);
} finally {
    readLock.unlock();
}
```

### Write Lock (Exclusive Lock)

- Only **one thread** can hold the write lock at a time
- While held, **no reader and no other writer** can proceed
- The writer has exclusive access to the shared state
- Functionally equivalent to a `ReentrantLock` in terms of exclusivity

```java
Lock writeLock = rwLock.writeLock();

writeLock.lock();
try {
    // Exclusive access — no readers or writers can be here
    sharedData.put(key, value);
} finally {
    writeLock.unlock();
}
```

### How they interact

- **Read lock blocks writers**: A writer must wait until ALL readers release the read lock
- **Write lock blocks everyone**: Both readers and writers must wait until the writer releases
- **Read lock does NOT block readers**: This is the entire purpose of ReadWriteLock

---

## 4. Lock Compatibility Matrix

```
┌──────────────────┬────────────────┬─────────────────┐
│  Requesting \     │ Read Lock Held │ Write Lock Held │
│  Lock Type        │  (by others)   │   (by others)   │
├──────────────────┼────────────────┼─────────────────┤
│ Read Lock         │   ✅ ALLOWED   │   ❌ BLOCKED    │
├──────────────────┼────────────────┼─────────────────┤
│ Write Lock        │   ❌ BLOCKED   │   ❌ BLOCKED    │
└──────────────────┴────────────────┴─────────────────┘
```

- **Read + Read** → ALLOWED (shared access, the whole point)
- **Read + Write** → BLOCKED (writer must wait for all readers)
- **Write + Read** → BLOCKED (readers must wait for writer)
- **Write + Write** → BLOCKED (mutual exclusion between writers)

---

## 5. ReentrantReadWriteLock — The Concrete Implementation

`ReentrantReadWriteLock` is the only standard implementation of `ReadWriteLock` in the JDK.

```java
import java.util.concurrent.locks.ReentrantReadWriteLock;

// Non-fair (default) — higher throughput, risk of starvation
ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

// Fair — prevents starvation, lower throughput
ReentrantReadWriteLock fairRwLock = new ReentrantReadWriteLock(true);

// Get the individual locks
ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();
ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();
```

### Key properties

| Property          | Value                                                                |
|-------------------|----------------------------------------------------------------------|
| Reentrant         | Yes — both read and write locks                                      |
| Fair mode         | Configurable (constructor parameter)                                 |
| Max readers       | 65535 (2^16 - 1) — limited by internal state encoding               |
| Max write reentrancy | 65535 (2^16 - 1)                                                 |
| Lock downgrade    | Supported (write → read)                                             |
| Lock upgrade      | NOT supported (read → write causes deadlock)                         |
| Conditions        | Only write lock supports `newCondition()`                            |
| Interruptible     | Both locks support `lockInterruptibly()`                             |
| Try lock          | Both locks support `tryLock()` and `tryLock(timeout, unit)`          |

### Utility methods

```java
rwLock.getReadLockCount();      // number of read locks held (NOT number of threads!)
rwLock.getReadHoldCount();      // current thread's read lock count
rwLock.isWriteLocked();         // is the write lock held by any thread?
rwLock.isWriteLockedByCurrentThread(); // does current thread hold write lock?
rwLock.getWriteHoldCount();     // current thread's write lock reentrant count
rwLock.getQueueLength();        // approximate threads waiting for either lock
```

---

## 6. Reentrancy in ReadWriteLock

Both the read lock and write lock are **reentrant** — the same thread can acquire the same
lock multiple times without deadlocking.

### Write lock reentrancy

Works exactly like `ReentrantLock`:

```java
writeLock.lock();    // hold count = 1
writeLock.lock();    // hold count = 2
writeLock.unlock();  // hold count = 1
writeLock.unlock();  // hold count = 0 → released
```

### Read lock reentrancy

A thread can acquire the read lock multiple times:

```java
readLock.lock();     // thread's read hold count = 1
readLock.lock();     // thread's read hold count = 2
readLock.unlock();   // thread's read hold count = 1
readLock.unlock();   // thread's read hold count = 0 → reader released
```

### Writer acquiring read lock (downgrading)

A thread holding the **write lock** can also acquire the **read lock**:

```java
writeLock.lock();
readLock.lock();   // ✅ Allowed — same thread holds write, can also take read
readLock.unlock();
writeLock.unlock();
```

This is the basis for **lock downgrading** (see next section).

---

## 7. Lock Downgrading and Upgrading

### Lock Downgrading (Supported ✅)

Lock downgrading means transitioning from a write lock to a read lock **without releasing
exclusivity in between**. The pattern:

```java
writeLock.lock();
try {
    // Perform the write
    sharedData = computeNewValue();

    // Downgrade: acquire read lock WHILE holding write lock
    readLock.lock();
} finally {
    writeLock.unlock(); // release write lock, still hold read lock
}
try {
    // Now holding only the read lock
    // Can safely read the data we just wrote
    // Other readers can also enter now, but no writer can
    return sharedData;
} finally {
    readLock.unlock();
}
```

**Why downgrade?** After a write, you may want to continue reading the value you just wrote
while allowing other readers in. Without downgrading, you'd release the write lock and then
acquire the read lock — but another writer could sneak in between and change the data.

### Lock Upgrading (NOT Supported ❌ — Causes Deadlock!)

Lock upgrading means acquiring a write lock while holding a read lock:

```java
readLock.lock();
writeLock.lock();   // ❌ DEADLOCK — the thread waits for all readers (including itself!)
```

**Why does this deadlock?** The write lock requires ALL read locks to be released. But the
current thread holds a read lock and is waiting for the write lock. It will never release
the read lock because it's blocked waiting for the write lock. Classic circular dependency.

If two threads both try to upgrade simultaneously, they will **both deadlock** — each holds
a read lock and waits for the other to release before the write lock can be granted.

**The safe alternative:** Release the read lock, then acquire the write lock. Accept that
another thread may write in between and re-validate your assumptions:

```java
readLock.lock();
try {
    Object value = sharedData.get(key);
    if (value == null) {
        readLock.unlock();  // must release read lock first
        writeLock.lock();
        try {
            // Re-check! Another thread may have written while we were unlocked
            value = sharedData.get(key);
            if (value == null) {
                value = computeValue(key);
                sharedData.put(key, value);
            }
        } finally {
            writeLock.unlock();
        }
        // Optionally re-acquire read lock here
        return value;
    }
    return value;
} finally {
    // Only unlock if still holding the read lock
    // (careful with the control flow above)
}
```

---

## 8. Fairness: Fair vs Non-Fair Mode

### Non-Fair Mode (Default)

```java
new ReentrantReadWriteLock(false); // or no argument
```

- A thread requesting a lock may **barge** ahead of waiting threads
- Readers can acquire the read lock even if writers are waiting (if no writer is active)
- **Higher throughput** under most workloads
- Risk of **writer starvation** under heavy read load

### Fair Mode

```java
new ReentrantReadWriteLock(true);
```

- Locks are granted in **approximately arrival order** (FIFO)
- If a writer is waiting, new readers must wait behind the writer
- **Prevents starvation** but significantly reduces throughput
- Read throughput drops because readers must queue behind waiting writers

### The heuristic in non-fair mode

Even in non-fair mode, the implementation applies a **writer-priority heuristic**: if the
first queued thread is a writer, new reader acquisitions may be blocked (to reduce writer
starvation). However, this is a heuristic, not a guarantee.

---

## 9. Writer Starvation Problem

In non-fair mode with a continuous stream of readers:

```
Timeline:
Reader-1 acquires read lock
    Reader-2 acquires read lock (allowed — concurrent read)
    Writer-1 requests write lock → BLOCKED (readers active)
        Reader-3 acquires read lock (non-fair: barges in ahead of writer!)
        Reader-1 releases
        Reader-4 acquires read lock (another barge!)
        Reader-2 releases
        Reader-5 acquires read lock
        ...
    Writer-1 is STILL waiting → STARVATION!
```

If new readers keep arriving before all existing readers release, the writer may
**never** acquire the lock.

### Solutions

1. **Use fair mode**: `new ReentrantReadWriteLock(true)` — guarantees FIFO ordering,
   no starvation. Trade-off: lower throughput.
2. **Use `StampedLock`**: Its optimistic read mode reduces the read-lock holding time,
   giving writers more chances to acquire the lock.
3. **Application-level batching**: Batch reads together and leave gaps for writers.
4. **Time-bounded reads**: Use `tryLock(timeout)` for the write lock and report failure
   if the writer can't get in within the deadline.

---

## 10. Internal Implementation (AQS)

`ReentrantReadWriteLock` is built on **AbstractQueuedSynchronizer (AQS)**, the same
framework that powers `ReentrantLock`, `Semaphore`, and `CountDownLatch`.

### The state variable trick

AQS has a single `int state` field (32 bits). `ReentrantReadWriteLock` splits it:

```
    ┌─────────────────────────────────────────┐
    │          32-bit state variable           │
    ├───────────────────┬─────────────────────┤
    │  Upper 16 bits    │  Lower 16 bits      │
    │  Read lock count  │  Write lock count   │
    │  (shared count)   │  (exclusive count)  │
    └───────────────────┴─────────────────────┘
```

- **Upper 16 bits** → number of read lock acquisitions (across ALL threads)
- **Lower 16 bits** → write lock hold count (single thread, reentrant count)
- Maximum value for each = 65535 (2^16 - 1)

### Bit manipulation

```java
static final int SHARED_SHIFT   = 16;
static final int SHARED_UNIT    = (1 << SHARED_SHIFT);     // 0x00010000
static final int MAX_COUNT      = (1 << SHARED_SHIFT) - 1; // 0x0000FFFF = 65535
static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1; // 0x0000FFFF

static int sharedCount(int c)    { return c >>> SHARED_SHIFT; }  // read count
static int exclusiveCount(int c) { return c & EXCLUSIVE_MASK; }  // write count
```

### Per-thread read hold count

Since the upper 16 bits only store the **total** read count, the implementation uses a
`ThreadLocal<HoldCounter>` to track each thread's individual read hold count. This is
needed for:
- Determining if the current thread holds any read locks (for reentrancy)
- Knowing how many times to unlock

### Write lock acquisition flow

1. Check state. If `state == 0` → CAS to set lower 16 bits, set `exclusiveOwnerThread`
2. If write lock is held by current thread → increment lower 16 bits (reentrant)
3. If read locks are held or write lock held by another thread → enqueue in CLH queue, park

### Read lock acquisition flow

1. If write lock is held by **another** thread → block
2. If write lock is held by **same** thread → allowed (supports downgrading)
3. Check reader should block (fair mode: check queue; non-fair: heuristic check)
4. CAS to increment upper 16 bits
5. Update ThreadLocal hold counter

---

## 11. Performance Characteristics

### When ReadWriteLock helps

ReadWriteLock provides a benefit when:
- **Reads significantly outnumber writes** (e.g., >10:1 ratio)
- **Read operations take non-trivial time** (long enough that parallelism matters)
- **Multiple reader threads** are contending for the lock

### When it does NOT help

- **Write-heavy workloads**: The write lock is exclusive, so you're back to serial access.
  Plus the ReadWriteLock has **higher overhead** than ReentrantLock due to the complex state
  management.
- **Very short critical sections**: If the read operation is a simple field read (nanoseconds),
  the lock acquisition overhead dominates. An exclusive lock or even `volatile` may be faster.
- **Low contention**: If threads rarely compete for the lock, the simpler ReentrantLock wins
  due to lower overhead.
- **Single reader thread**: No concurrent readers to benefit from shared locking.

### Overhead comparison

| Lock Type           | Uncontended Cost | Overhead Source                          |
|---------------------|------------------|------------------------------------------|
| `synchronized`      | ~20-50 ns        | Biased locking, thin lock, JVM-optimized |
| `ReentrantLock`     | ~30-60 ns        | CAS on state, volatile read              |
| `ReentrantReadWriteLock` | ~40-80 ns   | CAS on state, ThreadLocal read counter   |

Under heavy **read** contention with many threads, ReadWriteLock throughput >> ReentrantLock
because readers proceed in parallel. Under heavy **write** contention, ReadWriteLock
throughput < ReentrantLock due to higher per-operation overhead.

---

## 12. When to Use ReadWriteLock vs ReentrantLock vs synchronized

| Scenario                                      | Best Choice              | Why                                          |
|-----------------------------------------------|--------------------------|----------------------------------------------|
| Simple mutual exclusion                       | `synchronized`           | Simplest, JVM-optimized                      |
| Need tryLock, timeout, interruptible lock      | `ReentrantLock`          | Flexible lock API                            |
| Need multiple condition queues                 | `ReentrantLock`          | `newCondition()` support                     |
| Read-heavy, multiple reader threads            | `ReentrantReadWriteLock` | Concurrent reads                             |
| Read-heavy, need optimistic reads              | `StampedLock`            | Even lower read overhead                     |
| Write-heavy workload                           | `ReentrantLock`          | Lower overhead than RRWL for exclusive access |
| Need reentrancy + read/write separation        | `ReentrantReadWriteLock` | Only option with both                        |
| Lock-free read, rare writes, non-reentrant OK  | `StampedLock`            | Optimistic read avoids lock acquisition      |

### Decision flowchart

```
Is it a simple synchronized block?
  └─ Yes → Use synchronized
  └─ No → Do you need read/write separation?
            └─ No → Use ReentrantLock
            └─ Yes → Are reads >> writes?
                      └─ No → Use ReentrantLock (RRWL overhead not worth it)
                      └─ Yes → Do you need reentrancy or Conditions?
                                └─ Yes → ReentrantReadWriteLock
                                └─ No → StampedLock (better performance)
```

---

## 13. Real-World Use Cases

### 1. Thread-safe cache

The classic use case. Reads (cache hits) are far more frequent than writes (cache misses /
invalidations).

```java
public class ThreadSafeCache<K, V> {
    private final Map<K, V> cache = new HashMap<>();
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    public V get(K key) {
        rwLock.readLock().lock();
        try {
            return cache.get(key);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void put(K key, V value) {
        rwLock.writeLock().lock();
        try {
            cache.put(key, value);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public V computeIfAbsent(K key, Function<K, V> mappingFunction) {
        rwLock.readLock().lock();
        try {
            V value = cache.get(key);
            if (value != null) return value;
        } finally {
            rwLock.readLock().unlock();
        }
        // Not found — acquire write lock
        rwLock.writeLock().lock();
        try {
            // Double-check (another thread may have populated it)
            V value = cache.get(key);
            if (value == null) {
                value = mappingFunction.apply(key);
                cache.put(key, value);
            }
            return value;
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
```

### 2. Configuration store

Application configuration that is read by many threads on every request, but updated
rarely (on config reload).

### 3. In-memory database / data structure

Any shared data structure where concurrent reads are safe:
- Lookup tables
- Routing tables
- DNS cache
- Feature flag stores

### 4. File system metadata

Operating systems use read-write locks for inode and directory metadata — many processes
read file attributes concurrently, but writes (rename, delete) need exclusive access.

---

## 14. Best Practices and Pitfalls

### Best Practices

1. **Always use try-finally** — same as ReentrantLock:

```java
readLock.lock();
try {
    // read
} finally {
    readLock.unlock();
}
```

2. **Keep critical sections short** — especially write locks. Long-held write locks block
   all readers.

3. **Prefer read lock over write lock** — only use write lock when actually modifying data.

4. **Use lock downgrading** when you write then need to read — avoids a gap where another
   writer could change the data.

5. **Declare locks as `private final`** — prevent external code from interfering.

### Common Pitfalls

| Pitfall | Problem | Solution |
|---------|---------|----------|
| Attempting lock upgrade (read → write) | Deadlock | Release read lock first, re-acquire write lock, re-validate |
| Using write lock for reads | Loses all concurrency benefit | Use read lock for read-only operations |
| Very short read critical sections | Lock overhead > work done | Consider volatile, Atomic*, or StampedLock |
| Forgetting to unlock in exception path | Lock leak, eventual deadlock | Always use try-finally |
| Holding read lock during I/O or sleep | Blocks writers for extended time | Minimize lock hold time |
| Condition on read lock | `UnsupportedOperationException` | Only write lock supports `newCondition()` |

---

## 15. Interview Questions & Answers

### Q1: What is a ReadWriteLock and when would you use it?

**A:** `ReadWriteLock` maintains a pair of locks — a shared read lock and an exclusive write
lock. It's used when reads significantly outnumber writes and read operations can safely
execute concurrently. The read lock allows multiple threads to read simultaneously, while
the write lock provides exclusive access for mutations. Ideal for caches, configuration
stores, and lookup tables.

### Q2: Explain the lock compatibility matrix of ReadWriteLock.

**A:** Read-Read is compatible (multiple readers can proceed concurrently). Read-Write and
Write-Read are incompatible (writer must wait for all readers, readers must wait for writer).
Write-Write is incompatible (mutual exclusion between writers). This asymmetry is what
provides the performance benefit over exclusive locks for read-heavy workloads.

### Q3: What is lock downgrading? How does it work?

**A:** Lock downgrading is transitioning from a write lock to a read lock atomically — you
acquire the read lock *while still holding the write lock*, then release the write lock.
This ensures no other writer can sneak in between the write and subsequent read. It's
used when a thread writes data and then needs to continue reading it while allowing
other readers to proceed.

### Q4: Why is lock upgrading (read → write) not supported? What happens if you try?

**A:** Lock upgrading causes deadlock. The write lock requires ALL read locks to be
released, but the current thread holds a read lock and is blocked waiting for the write
lock — it will never release its read lock because it's waiting. If two threads attempt
to upgrade simultaneously, both deadlock (each holds a read lock the other needs released).
The safe approach: release read lock, acquire write lock, then re-validate your assumptions.

### Q5: How does ReentrantReadWriteLock work internally?

**A:** It's built on AQS (AbstractQueuedSynchronizer). The 32-bit `state` variable is split:
upper 16 bits store the read lock count (total across all threads), lower 16 bits store the
write lock hold count. This limits both to a max of 65535. Per-thread read hold counts are
tracked via `ThreadLocal<HoldCounter>`. Write lock acquisition uses exclusive mode CAS; read
lock acquisition uses shared mode CAS on the upper bits.

### Q6: What is writer starvation? How do you prevent it?

**A:** In non-fair mode, if readers arrive continuously, a waiting writer may never acquire
the write lock because new readers keep barging in while existing readers still hold the lock.
Solutions: (1) use fair mode (`new ReentrantReadWriteLock(true)`) to enforce FIFO ordering,
(2) use `StampedLock` with optimistic reads that reduce read-lock holding time, or (3)
implement application-level writer priority.

### Q7: Can the read lock's `newCondition()` be called?

**A:** No. Calling `readLock().newCondition()` throws `UnsupportedOperationException`. Only
the write lock supports conditions because condition variables require exclusive ownership
of the lock (you must hold the write lock to `await()` or `signal()`), and the read lock
is shared.

### Q8: How does fair mode affect performance?

**A:** Fair mode enforces approximate FIFO ordering. When a writer is queued, new readers
must queue behind it rather than barging in. This prevents writer starvation but significantly
reduces read throughput because readers can no longer proceed concurrently when a writer is
waiting. In benchmarks, fair RRWL can be 2-10x slower than non-fair under read-heavy contention.

### Q9: Compare ReentrantReadWriteLock with StampedLock.

**A:** Key differences:
- **Reentrancy**: RRWL is reentrant; StampedLock is NOT
- **Optimistic reading**: StampedLock supports lock-free optimistic reads; RRWL does not
- **Conditions**: RRWL write lock supports conditions; StampedLock does not
- **Lock conversion**: StampedLock supports `tryConvertToWriteLock()` (safe upgrade);
  RRWL cannot upgrade
- **Performance**: StampedLock is faster under read-heavy workloads due to optimistic reads
- **Complexity**: StampedLock is harder to use correctly (stamps, no reentrancy, no try-with-resources)

### Q10: Design a thread-safe LRU cache. Which lock would you choose?

**A:** For an LRU cache, every `get()` operation must also update the access order (move
the entry to the front), which is a **write** operation. This means even reads require
mutual exclusion, making `ReadWriteLock` less beneficial. Options:
- `ConcurrentHashMap` + striped ordering (like Caffeine/Guava does) for best performance
- `ReentrantLock` if access-order tracking is required on every read
- `ReadWriteLock` only if you separate the access-order update from the read (e.g., using
  a write-behind approach with a concurrent queue for access events)

This is a common interview trap — candidates suggest ReadWriteLock for a cache, but LRU
semantics make every access a write.

---

## Quick Reference Cheat Sheet

```java
// Create
ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();         // non-fair
ReentrantReadWriteLock fairLock = new ReentrantReadWriteLock(true);   // fair

// Read lock
rwLock.readLock().lock();
try { /* concurrent read */ } finally { rwLock.readLock().unlock(); }

// Write lock
rwLock.writeLock().lock();
try { /* exclusive write */ } finally { rwLock.writeLock().unlock(); }

// Lock downgrade
rwLock.writeLock().lock();
try {
    // write
    rwLock.readLock().lock();  // acquire read while holding write
} finally {
    rwLock.writeLock().unlock(); // release write, keep read
}
try {
    // read (other readers allowed, writers blocked)
} finally {
    rwLock.readLock().unlock();
}

// Try lock
if (rwLock.readLock().tryLock()) {
    try { /* read */ } finally { rwLock.readLock().unlock(); }
}

// Timed lock
if (rwLock.writeLock().tryLock(5, TimeUnit.SECONDS)) {
    try { /* write */ } finally { rwLock.writeLock().unlock(); }
}

// Utility methods
rwLock.getReadLockCount();               // total read acquisitions
rwLock.getReadHoldCount();               // current thread's read count
rwLock.isWriteLocked();                  // write lock held?
rwLock.isWriteLockedByCurrentThread();   // current thread holds write lock?
rwLock.getWriteHoldCount();              // current thread's write reentrant count
rwLock.getQueueLength();                 // threads waiting
```
