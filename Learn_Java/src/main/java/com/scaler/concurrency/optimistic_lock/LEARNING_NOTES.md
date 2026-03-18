# Optimistic Locking — Complete Deep Dive for SDE2

## Table of Contents

1. [What is Optimistic Locking?](#1-what-is-optimistic-locking)
2. [Optimistic Locking is a Strategy, Not a Class](#2-optimistic-locking-is-a-strategy-not-a-class)
3. [How Optimistic Locking Works — Step by Step](#3-how-optimistic-locking-works--step-by-step)
4. [Java Implementations](#4-java-implementations)
5. [CAS (Compare-And-Swap) — The Foundation](#5-cas-compare-and-swap--the-foundation)
6. [The ABA Problem](#6-the-aba-problem)
7. [Optimistic vs Pessimistic — When to Use Each](#7-optimistic-vs-pessimistic--when-to-use-each)
8. [Retry Strategies](#8-retry-strategies)
9. [Database Optimistic Locking](#9-database-optimistic-locking)
10. [Lock-Free Data Structures](#10-lock-free-data-structures)
11. [Real-World Examples](#11-real-world-examples)
12. [Interview Questions & Answers](#12-interview-questions--answers)

---

## 1. What is Optimistic Locking?

**Optimistic locking** is a concurrency control strategy based on the assumption that
**conflicts are UNLIKELY**. Instead of locking the resource before access, you:
1. Read the data (and note a version/stamp)
2. Do your work
3. At commit time, check if anyone else modified the data
4. If no conflict → commit. If conflict → retry.

### Formal Definition

> Optimistic locking is a concurrency control mechanism where a thread reads data along
> with a **version identifier** (counter, timestamp, or hash), performs computations or
> modifications, then **validates** that the version has not changed before committing the
> result. If the version has changed (indicating another thread modified the data), the
> operation is **retried** or **aborted**.

### The Optimistic Mindset

```
"I assume nobody ELSE will modify this data while I'm working on it.
 I'll read it, do my work, and at the end, CHECK if anyone changed it.
 If someone did, I'll just try again. No big deal — conflicts are rare."
```

### Analogy

Think of a **Google Doc without real-time sync**:
- You open the document and start editing (no lock)
- When you try to save, the system checks if anyone else edited since you opened it
- If no one else edited → save succeeds
- If someone else edited → conflict! You must merge or retry your changes

Compare with **pessimistic locking** (a Google Doc that locks the file):
- You "check out" the document. Nobody else can edit.
- You make changes, save, and "check in."
- No conflicts possible, but others must wait.

---

## 2. Optimistic Locking is a Strategy, Not a Class

Like pessimistic locking, optimistic locking is a **strategy/pattern**, not a specific
Java class.

> **Key Insight**: Optimistic locking is about the APPROACH, not the implementation.
> Any mechanism that follows the "read-compute-validate-commit" pattern is optimistic.

### Java Tools for Optimistic Locking

| Tool                           | Level          | How It's Optimistic                          |
|--------------------------------|----------------|----------------------------------------------|
| `AtomicInteger`                | JUC class      | CAS: compare-and-swap (read, modify, CAS)   |
| `AtomicLong`                   | JUC class      | Same as AtomicInteger for longs              |
| `AtomicReference<V>`           | JUC class      | CAS on object references                     |
| `AtomicStampedReference<V>`    | JUC class      | CAS with version stamp (solves ABA)          |
| `StampedLock.tryOptimisticRead()` | JUC class   | Optimistic read with stamp validation        |
| `LongAdder`, `LongAccumulator`| JUC class      | Striped CAS for high-throughput counting     |
| `@Version` (JPA)              | ORM annotation | Version column checked at commit time        |
| Version column (SQL)           | Database       | WHERE version = ? in UPDATE statement        |

---

## 3. How Optimistic Locking Works — Step by Step

### The Fundamental Pattern

```
Thread-A wants to modify shared resource R:

Step 1: READ data from R + capture VERSION
        ┌─────────────────────────────────────┐
        │  value = R.getValue()                │
        │  version = R.getVersion()            │
        │  (No lock acquired!)                 │
        └─────────────────────────────────────┘

Step 2: COMPUTE the new value
        ┌─────────────────────────────────────┐
        │  newValue = f(value)                 │
        │  (Done without holding any lock)     │
        └─────────────────────────────────────┘

Step 3: VALIDATE + COMMIT (atomically)
        ┌─────────────────────────────────────┐
        │  if (R.version == version) {         │  ← "Has anyone changed it?"
        │      R.setValue(newValue)             │
        │      R.version++                     │
        │      → SUCCESS                       │
        │  } else {                            │
        │      → FAILURE (conflict detected)   │
        │      → RETRY from Step 1             │
        │  }                                   │
        └─────────────────────────────────────┘
```

### Execution Timeline — No Conflict

```
Thread-A: read(version=1) → compute → CAS(version==1? → YES) → COMMIT (version=2)
Thread-B: ──────────────────────── read(version=2) → compute → CAS(version==2? → YES) → COMMIT (version=3)

No conflict: both succeed on first try. Zero blocking.
```

### Execution Timeline — With Conflict

```
Thread-A: read(version=1) → compute → CAS(version==1? → YES) → COMMIT (version=2)
Thread-B: read(version=1) → compute → CAS(version==1? → NO, it's 2!) → RETRY
Thread-B: read(version=2) → compute → CAS(version==2? → YES) → COMMIT (version=3)

Thread-B wasted its first compute, but succeeded on retry.
```

---

## 4. Java Implementations

### 4.1 AtomicInteger — CAS-Based Counter

The most common optimistic locking primitive in Java:

```java
AtomicInteger counter = new AtomicInteger(0);

// Optimistic increment — retry loop
int oldValue, newValue;
do {
    oldValue = counter.get();           // Step 1: read
    newValue = oldValue + 1;            // Step 2: compute
} while (!counter.compareAndSet(oldValue, newValue));  // Step 3: CAS
// Equivalent shorthand: counter.incrementAndGet();
```

`compareAndSet(expected, update)`:
- If current value == expected → set to update, return true (SUCCESS)
- If current value != expected → do nothing, return false (CONFLICT)

### 4.2 AtomicReference — CAS on Objects

```java
AtomicReference<String> ref = new AtomicReference<>("initial");

String oldVal, newVal;
do {
    oldVal = ref.get();
    newVal = oldVal.toUpperCase();
} while (!ref.compareAndSet(oldVal, newVal));
```

### 4.3 StampedLock — Optimistic Read Mode

`StampedLock` (Java 8+) provides a unique optimistic read that doesn't acquire any lock:

```java
StampedLock stampedLock = new StampedLock();
double x, y;

// Writer: acquires exclusive write lock (pessimistic)
long writeStamp = stampedLock.writeLock();
try {
    x = newX;
    y = newY;
} finally {
    stampedLock.unlockWrite(writeStamp);
}

// Reader: optimistic read (NO lock acquired!)
long stamp = stampedLock.tryOptimisticRead();   // returns a stamp, no lock!
double localX = x;
double localY = y;
if (!stampedLock.validate(stamp)) {
    // A writer acquired the lock between tryOptimisticRead and validate.
    // Fall back to pessimistic read lock.
    stamp = stampedLock.readLock();
    try {
        localX = x;
        localY = y;
    } finally {
        stampedLock.unlockRead(stamp);
    }
}
// Use localX, localY
```

**How it works:**
1. `tryOptimisticRead()` returns the current stamp (lock version) without acquiring any lock
2. Read the shared variables
3. `validate(stamp)` checks if any writer acquired the lock since the stamp was obtained
4. If valid → the reads are consistent, use them
5. If invalid → a writer intervened, fall back to pessimistic read

### 4.4 AtomicStampedReference — CAS with Version

Adds a stamp (integer version) to prevent the ABA problem:

```java
AtomicStampedReference<String> ref = new AtomicStampedReference<>("A", 0);

int[] stampHolder = new int[1];
String current = ref.get(stampHolder);
int currentStamp = stampHolder[0];

boolean success = ref.compareAndSet(
    current, "B",           // expected → new value
    currentStamp, currentStamp + 1  // expected stamp → new stamp
);
```

---

## 5. CAS (Compare-And-Swap) — The Foundation

CAS is the **fundamental hardware primitive** that enables optimistic locking. It is a
single, **atomic CPU instruction**.

### How CAS Works

```
CAS(memoryLocation, expectedValue, newValue):
  ATOMICALLY do:
    if (*memoryLocation == expectedValue) {
        *memoryLocation = newValue
        return true    // SUCCESS
    } else {
        return false   // FAILURE — someone else changed it
    }
```

### CAS at the CPU Level

On x86 processors, CAS is the `CMPXCHG` (Compare and Exchange) instruction:

```
LOCK CMPXCHG [destination], source
```

- `LOCK` prefix: ensures the operation is atomic (locks the cache line)
- `CMPXCHG`: compares accumulator (EAX) with destination; if equal, loads source into
  destination; otherwise, loads destination into accumulator

### CAS in Java

Java's `Unsafe` class (internal) provides CAS. The `Atomic*` classes wrap it:

```java
// Under the hood, AtomicInteger.compareAndSet does:
public final boolean compareAndSet(int expect, int update) {
    return U.compareAndSetInt(this, VALUE_OFFSET, expect, update);
}
// U = jdk.internal.misc.Unsafe
// VALUE_OFFSET = memory offset of the `value` field
// This compiles to a single LOCK CMPXCHG instruction on x86
```

### CAS vs Locking — Performance

| Operation              | Approximate Cost |
|------------------------|-----------------|
| CAS (no contention)   | ~5-15 ns        |
| CAS (contention)      | ~50-100 ns      |
| synchronized (biased) | ~5-20 ns        |
| synchronized (thin)   | ~20-50 ns       |
| synchronized (fat/contended) | ~1-10 μs (context switch) |
| ReentrantLock          | ~50-100 ns (uncontended) |

Under no contention, CAS is the fastest. Under heavy contention, CAS degrades
(many failed attempts), but still avoids context switches.

### The CAS Retry Loop Pattern

The standard pattern for optimistic updates:

```java
// Generic CAS retry loop
AtomicInteger value = new AtomicInteger(0);

int expected, desired;
do {
    expected = value.get();          // read current
    desired = transform(expected);   // compute new value
} while (!value.compareAndSet(expected, desired));  // atomic commit
```

If CAS fails (another thread changed the value), we retry with the new current value.
This is the optimistic locking pattern: read → compute → try-commit → retry if failed.

---

## 6. The ABA Problem

The **ABA problem** is the most important pitfall of CAS-based optimistic locking.

### What is the ABA Problem?

CAS checks: "is the current value equal to the expected value?" But it cannot detect if
the value **changed and then changed back** to the original.

```
Timeline:
  Thread-1: reads value = A
  Thread-2: changes value A → B
  Thread-2: changes value B → A
  Thread-1: CAS(expected=A, new=C) → SUCCEEDS! (value is A, as expected)

Thread-1 thinks nothing changed, but the value went A → B → A.
This can be a problem if Thread-1's operation depends on the value NOT
having changed at all (not just the final value being the same).
```

### When ABA is Dangerous

ABA is dangerous in **pointer-based** or **linked** data structures:

```
Lock-free stack (Treiber Stack):
  Initial: TOP → [A] → [B] → [C]

  Thread-1: wants to pop. Reads TOP=A, next=B. Will CAS(TOP, A, B).
  Thread-1: gets preempted...

  Thread-2: pops A.     TOP → [B] → [C]
  Thread-2: pops B.     TOP → [C]
  Thread-2: pushes A.   TOP → [A] → [C]    ← A is back on top!

  Thread-1: resumes. CAS(TOP, A, B) → SUCCEEDS!
  But B was already popped! TOP now points to freed/invalid memory.

  Result: TOP → [B] → ??? (data corruption)
```

### When ABA is NOT a Problem

For simple counters, ABA is usually harmless:

```java
// Counter: value goes 5 → 6 → 5 → then we CAS 5 → 6
// Result is still correct because we only care about incrementing.
AtomicInteger counter = new AtomicInteger(5);
// Even if value went 5 → 6 → 5, CAS(5, 6) gives the right answer.
```

### Solution 1: AtomicStampedReference

Adds a **version stamp** alongside the reference. CAS checks BOTH the reference AND the
stamp. Even if the value goes A → B → A, the stamp changes (0 → 1 → 2), so CAS detects
the change.

```java
AtomicStampedReference<String> ref = new AtomicStampedReference<>("A", 0);

// Read value + stamp
int[] stampHolder = new int[1];
String value = ref.get(stampHolder);
int stamp = stampHolder[0];

// Later: CAS checks both value AND stamp
boolean success = ref.compareAndSet(
    "A", "C",           // expected value → new value
    stamp, stamp + 1    // expected stamp → new stamp
);
// If value went A → B → A, stamp went 0 → 1 → 2.
// Our stamp is still 0, so CAS FAILS — ABA detected!
```

### Solution 2: AtomicMarkableReference

A simpler variant that uses a **boolean mark** instead of an integer stamp:

```java
AtomicMarkableReference<String> ref = new AtomicMarkableReference<>("A", false);

boolean[] markHolder = new boolean[1];
String value = ref.get(markHolder);
boolean mark = markHolder[0];

boolean success = ref.compareAndSet(
    "A", "C",          // expected value → new value
    false, true         // expected mark → new mark
);
```

### ABA Prevention Comparison

| Approach                    | Detects ABA? | Overhead      | Use Case                  |
|-----------------------------|-------------|---------------|---------------------------|
| Plain CAS (`AtomicInteger`) | No          | Lowest        | Simple counters           |
| `AtomicStampedReference`    | Yes         | Medium        | Pointer-based structures  |
| `AtomicMarkableReference`   | Partially   | Medium        | Soft-delete markers       |
| Hazard Pointers             | Yes         | Higher        | Lock-free memory mgmt     |
| Epoch-Based Reclamation     | Yes         | Medium        | Lock-free memory mgmt     |

---

## 7. Optimistic vs Pessimistic — When to Use Each

### Quick Decision Guide

```
Is contention LOW (< 10-20% of operations conflict)?
  → YES → Use OPTIMISTIC (CAS, AtomicInteger, @Version)
  → NO  → How expensive is a retry?
            → CHEAP (simple computation) → Optimistic may still work
            → EXPENSIVE (I/O, API calls) → Use PESSIMISTIC
```

### Detailed Comparison

| Scenario                              | Best Choice     | Why                                          |
|---------------------------------------|-----------------|----------------------------------------------|
| Counter increments, few threads       | Optimistic      | CAS is 5-10x faster than lock/unlock         |
| Counter increments, 100 threads       | Optimistic      | LongAdder handles contention via striping     |
| Bank transfer (debit + credit)        | Pessimistic     | Multi-step operation, retrying is risky       |
| Read-heavy cache (95% reads)          | Optimistic      | StampedLock optimistic read, zero overhead    |
| Inventory decrement (flash sale)      | Pessimistic     | High contention, retries cause thundering herd|
| Database row update (low traffic)     | Optimistic      | @Version column, conflicts rare               |
| Database row update (high traffic)    | Pessimistic     | SELECT FOR UPDATE prevents retry storms       |
| Lock-free queue                       | Optimistic      | CAS-based, high throughput                    |
| Thread pool management                | Pessimistic     | Complex state transitions, hard to retry      |

### The Contention Spectrum

```
Low Contention ◀────────────────────────────────────▶ High Contention
  Optimistic ✓✓✓           Either             Pessimistic ✓✓✓

  AtomicInteger          ReentrantLock        synchronized
  StampedLock            with tryLock         ReentrantLock
  @Version                                    SELECT FOR UPDATE
```

---

## 8. Retry Strategies

When an optimistic operation fails (CAS returns false, version mismatch), you must retry.
The retry strategy significantly affects performance.

### 8.1 Spin Retry (Tight Loop)

```java
AtomicInteger counter = new AtomicInteger(0);
int oldVal, newVal;
do {
    oldVal = counter.get();
    newVal = oldVal + 1;
} while (!counter.compareAndSet(oldVal, newVal));
```

- **Best for**: very short operations where success is expected within a few tries
- **Problem**: burns CPU cycles spinning

### 8.2 Yield Retry

```java
int oldVal, newVal;
while (true) {
    oldVal = counter.get();
    newVal = oldVal + 1;
    if (counter.compareAndSet(oldVal, newVal)) break;
    Thread.yield();  // hint to scheduler to let other threads run
}
```

- **Best for**: moderate contention where giving up the CPU briefly helps

### 8.3 Exponential Backoff

```java
int backoff = 1;
while (true) {
    int oldVal = counter.get();
    int newVal = oldVal + 1;
    if (counter.compareAndSet(oldVal, newVal)) break;

    try {
        Thread.sleep(backoff);
        backoff = Math.min(backoff * 2, 100);  // cap at 100ms
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
    }
}
```

- **Best for**: high contention where immediate retry would just fail again
- **Used in**: network protocols, distributed locks, CAS with high contention

### 8.4 Limited Retries with Fallback

```java
int maxRetries = 10;
for (int i = 0; i < maxRetries; i++) {
    int oldVal = counter.get();
    int newVal = oldVal + 1;
    if (counter.compareAndSet(oldVal, newVal)) return;
}
// Fallback to pessimistic locking
synchronized (this) {
    // guaranteed to succeed
}
```

- **Best for**: hybrid approach where optimistic is tried first, pessimistic is fallback
- **Used in**: StampedLock (optimistic read → fallback to read lock)

---

## 9. Database Optimistic Locking

### Version Column Pattern

The most common database-level optimistic locking uses a `version` column:

```sql
-- Table structure
CREATE TABLE products (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255),
    price DECIMAL(10,2),
    version INT NOT NULL DEFAULT 0
);

-- Read
SELECT id, name, price, version FROM products WHERE id = 42;
-- Returns: (42, 'Widget', 9.99, 3)

-- Update with version check (optimistic locking)
UPDATE products
SET price = 10.99, version = version + 1
WHERE id = 42 AND version = 3;
-- If 1 row updated → SUCCESS
-- If 0 rows updated → CONFLICT (someone else updated version)
```

### JPA/Hibernate @Version

```java
@Entity
public class Product {
    @Id
    private Long id;

    private String name;
    private BigDecimal price;

    @Version
    private Integer version;  // Hibernate manages this automatically
}

// Usage
Product product = entityManager.find(Product.class, 42L);
product.setPrice(new BigDecimal("10.99"));
entityManager.merge(product);  // Hibernate adds: WHERE version = ? AND increments version
// If version mismatch → throws OptimisticLockException
```

### Timestamp-Based Optimistic Locking

Instead of an integer version, use a timestamp:

```java
@Entity
public class Product {
    @Version
    private Timestamp lastModified;  // Hibernate uses timestamp as version
}
```

### Version vs Timestamp

| Approach     | Pros                                | Cons                                  |
|-------------|-------------------------------------|---------------------------------------|
| Integer version | Simple, always incrementing, no clock issues | No meaningful time info      |
| Timestamp   | Provides "last modified" info       | Clock skew issues, less precise       |

**Recommendation**: Use integer version for optimistic locking. Add a separate
`last_modified` column if you need time information.

---

## 10. Lock-Free Data Structures

Optimistic locking (CAS) enables **lock-free** data structures that provide thread safety
without any blocking.

### What is Lock-Free?

> A data structure is **lock-free** if at least one thread is guaranteed to make progress
> in a finite number of steps, regardless of what other threads do. It ensures system-wide
> progress even if individual threads may retry.

### Java's Lock-Free Data Structures

| Class                       | Based On            | Key Feature                           |
|-----------------------------|---------------------|---------------------------------------|
| `ConcurrentLinkedQueue`     | Michael-Scott queue | Non-blocking FIFO queue               |
| `ConcurrentLinkedDeque`     | CAS-based deque     | Non-blocking double-ended queue       |
| `ConcurrentSkipListMap`     | Skip list + CAS     | Sorted concurrent map (like TreeMap)  |
| `ConcurrentSkipListSet`     | Skip list + CAS     | Sorted concurrent set                 |
| `ConcurrentHashMap`         | CAS + synchronized  | Hybrid: CAS for reads, lock for writes|
| `LongAdder`                 | Striped CAS         | High-throughput counter               |

### ConcurrentLinkedQueue — Lock-Free Queue

```java
ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
queue.offer("item1");    // CAS-based enqueue (no lock)
String item = queue.poll();  // CAS-based dequeue (no lock)
```

Internally uses a **Michael-Scott algorithm**: CAS to update the tail pointer for enqueue,
CAS to update the head pointer for dequeue.

### LongAdder — Striped Optimistic Counting

When `AtomicLong` has too much contention (many threads CAS on the same variable),
`LongAdder` uses **striping**: each thread updates a different cell, and the final sum
is computed on demand.

```java
LongAdder adder = new LongAdder();
adder.increment();      // CAS on a stripe (thread-local cell)
adder.increment();
long total = adder.sum();  // sum of all stripes
```

This is still optimistic (CAS-based) but reduces contention by spreading writes across
multiple cells.

---

## 11. Real-World Examples

### 11.1 Git — Merge Conflicts as Optimistic Locking

Git is a real-world optimistic locking system:
1. You `git pull` (read the current version)
2. You make changes (compute)
3. You `git push` (try to commit)
4. If someone else pushed → **conflict** → you must `git pull` and merge (retry)

No file is locked. Conflicts are detected after the fact.

### 11.2 Database ORM Versioning

JPA/Hibernate `@Version` is used in virtually every enterprise Java application:
```java
Product product = repository.findById(42L);
product.setPrice(newPrice);
repository.save(product);  // version check happens automatically
// If another transaction modified the product → OptimisticLockException
```

### 11.3 CAS-Based Counters (Metrics, Rate Limiting)

```java
AtomicLong requestCount = new AtomicLong(0);
requestCount.incrementAndGet();  // every HTTP request
// CAS-based, lock-free, extremely fast
```

### 11.4 HTTP ETags

ETags are optimistic locking for HTTP resources:
```
GET /api/product/42
→ ETag: "version-7"

PUT /api/product/42
If-Match: "version-7"    ← "I'm basing my update on version 7"
→ 200 OK (if still version 7)
→ 412 Precondition Failed (if version changed → retry)
```

### 11.5 ConcurrentHashMap Internal Optimization

`ConcurrentHashMap` uses optimistic reads (no lock for `get()`) and pessimistic writes
(`synchronized` on the bucket for `put()`). This hybrid approach gives:
- Reads: lock-free, O(1), highly concurrent
- Writes: locked per-bucket, fine-grained concurrency

---

## 12. Interview Questions & Answers

### Q1: What is optimistic locking and how does it differ from pessimistic locking?

**A:** Optimistic locking assumes conflicts are unlikely. It reads data with a version,
performs operations, then validates the version hasn't changed before committing. If it
changed (conflict), the operation retries. Pessimistic locking assumes conflicts are
likely and acquires an exclusive lock before accessing data. Optimistic has zero overhead
when no conflicts occur (no lock/unlock cost) but wastes work on retries. Pessimistic
always has lock overhead but never wastes work.

---

### Q2: Explain CAS (Compare-And-Swap) and how it enables optimistic locking.

**A:** CAS is an atomic CPU instruction that takes three operands: memory location,
expected value, and new value. It atomically checks if the memory location contains the
expected value; if so, it replaces it with the new value and returns true; otherwise it
returns false without modifying anything. In Java, `AtomicInteger.compareAndSet()` wraps
this instruction. The optimistic pattern is: read the current value, compute the new value,
then CAS. If CAS fails (another thread changed the value), retry. This is non-blocking —
no thread ever waits for a lock.

---

### Q3: What is the ABA problem and how do you solve it?

**A:** The ABA problem occurs when a value changes from A to B and back to A. CAS only
checks the current value, so it thinks nothing changed, when in fact the value was
modified twice. This is dangerous in pointer-based structures where intermediate changes
may have invalidated assumptions. Solutions: (1) `AtomicStampedReference` — pairs the
value with an ever-incrementing stamp; CAS checks both value AND stamp, so A→B→A with
stamps 0→1→2 is detected. (2) `AtomicMarkableReference` — simpler variant with a boolean
mark. (3) Hazard pointers or epoch-based reclamation for memory management in lock-free
structures.

---

### Q4: How does StampedLock's optimistic read work?

**A:** `StampedLock.tryOptimisticRead()` returns a stamp (version number) without acquiring
any lock. You then read the shared data. After reading, call `stampedLock.validate(stamp)`
to check if a writer acquired the lock between `tryOptimisticRead` and `validate`. If
valid, the reads are consistent. If invalid, fall back to a pessimistic read lock. This
is the fastest read path for read-heavy workloads because successful optimistic reads
have zero synchronization overhead — no lock acquired or released.

---

### Q5: When would you choose AtomicInteger over synchronized for a counter?

**A:** `AtomicInteger` uses CAS (optimistic) while `synchronized` uses a monitor lock
(pessimistic). AtomicInteger is faster under low-to-moderate contention because CAS is
~5-15ns vs ~20-50ns for synchronized. However, under extremely high contention (64+
threads incrementing the same counter), CAS degrades due to many retries. In that case,
`LongAdder` (striped CAS) is better. Use `synchronized` when the critical section contains
multiple operations beyond a single CAS, or when you need condition variables.

---

### Q6: Explain database optimistic locking with a version column.

**A:** A `version` integer column is added to the table. On read, the version is fetched.
On update, the WHERE clause includes `AND version = ?` and the SET clause increments it.
If the UPDATE affects 0 rows, a concurrent modification occurred. In JPA, annotate a field
with `@Version` and Hibernate handles this automatically — it throws
`OptimisticLockException` on version mismatch. This avoids `SELECT FOR UPDATE` and its
blocking behavior, giving higher throughput under low write contention.

---

### Q7: What are lock-free data structures and how do they relate to optimistic locking?

**A:** Lock-free data structures guarantee system-wide progress without using locks. They
rely on CAS (optimistic locking) internally. For example, `ConcurrentLinkedQueue` uses
CAS to update head/tail pointers. If CAS fails (another thread modified the pointer), it
retries. The "lock-free" guarantee means at least one thread makes progress in any finite
window — no deadlock, no lock convoy. Java provides several: `ConcurrentLinkedQueue`,
`ConcurrentSkipListMap`, `LongAdder`. They trade simplicity for higher concurrency.

---

### Q8: What is the difference between lock-free and wait-free?

**A:** **Lock-free**: at least one thread makes progress in any finite period (system-wide
progress, but individual threads may starve). **Wait-free**: every thread makes progress
in a bounded number of steps (per-thread progress guarantee, no starvation). Wait-free is
strictly stronger. Most Java concurrent classes are lock-free but not wait-free. Wait-free
algorithms exist but are complex and rarely used in practice due to overhead.

---

### Q9: Explain the CAS retry loop pattern and its performance characteristics.

**A:** The pattern is: `do { read; compute; } while (!CAS());` Each iteration reads the
current value, computes the new value, and attempts CAS. If CAS fails, the loop retries
with the updated value. Performance: under no contention, succeeds on first CAS (~5-15ns).
Under moderate contention, retries 1-3 times. Under heavy contention, many threads
spinning on CAS cause cache line bouncing and throughput degrades. Mitigation: use
exponential backoff between retries, or switch to striped approaches like `LongAdder`.

---

### Q10: How does ConcurrentHashMap combine optimistic and pessimistic locking?

**A:** `ConcurrentHashMap` uses a hybrid approach: reads (`get()`) are fully optimistic —
they use volatile reads with no locking, achieving high concurrency. Writes (`put()`)
use fine-grained pessimistic locking — they `synchronized` on the specific bucket (bin)
being modified. This means reads never block and can proceed concurrently with writes.
Writes only block other writes to the same bucket. The size tracking uses `LongAdder`-style
striped counters for high-throughput counting. This hybrid design gives excellent
read performance while ensuring write safety.
