# Pessimistic Locking — Complete Deep Dive for SDE2

## Table of Contents

1. [What is Pessimistic Locking?](#1-what-is-pessimistic-locking)
2. [Pessimistic Locking is a Strategy, Not a Class](#2-pessimistic-locking-is-a-strategy-not-a-class)
3. [Java Implementations of Pessimistic Locking](#3-java-implementations-of-pessimistic-locking)
4. [How Pessimistic Locking Works — Step by Step](#4-how-pessimistic-locking-works--step-by-step)
5. [Pessimistic vs Optimistic Locking — Detailed Comparison](#5-pessimistic-vs-optimistic-locking--detailed-comparison)
6. [When to Use Pessimistic Locking](#6-when-to-use-pessimistic-locking)
7. [Database Pessimistic Locking](#7-database-pessimistic-locking)
8. [Disadvantages of Pessimistic Locking](#8-disadvantages-of-pessimistic-locking)
9. [Real-World Examples](#9-real-world-examples)
10. [Interview Questions & Answers](#10-interview-questions--answers)

---

## 1. What is Pessimistic Locking?

**Pessimistic locking** is a concurrency control strategy based on the assumption that
**conflicts WILL happen**. Therefore, you **lock the resource BEFORE accessing it** to
guarantee exclusive access.

### Formal Definition

> Pessimistic locking is a concurrency control mechanism where a thread/process acquires
> an exclusive lock on a shared resource **before** reading or modifying it, holds the lock
> for the **entire duration** of the operation, and releases it only **after** the operation
> is complete. No other thread can access the resource while the lock is held.

### The Pessimistic Mindset

```
"I assume someone ELSE will try to modify this data at the same time.
 Therefore, I will LOCK it FIRST, do my work, then release it.
 Nobody can interfere with me while I hold the lock."
```

### Analogy

Think of a **bathroom with a lock on the door**:
- You LOCK the door before using it (acquire lock before access)
- Nobody else can enter while you're inside (exclusive access)
- You UNLOCK when you're done (release lock after operation)
- You don't check if someone else used it — you **prevented** any interference

Compare with **optimistic locking** (a bathroom without a lock):
- You walk in and start using it
- If someone else is there, you **back out and try again** later
- No prevention — you **detect** the conflict after the fact

---

## 2. Pessimistic Locking is a Strategy, Not a Class

This is a crucial distinction for interviews:

> **Pessimistic locking is NOT a specific Java class or API.**
> It is a **strategy/pattern** for concurrency control.

Java provides several tools that can be used to implement pessimistic locking:

| Tool                          | Level          | How It's Pessimistic                      |
|-------------------------------|----------------|-------------------------------------------|
| `synchronized`                | JVM keyword    | Implicit lock before entering block       |
| `ReentrantLock`               | JUC class      | Explicit lock()/unlock()                  |
| `ReentrantReadWriteLock`      | JUC class      | Write lock = pessimistic exclusive access  |
| `StampedLock.writeLock()`     | JUC class      | Exclusive write locking                   |
| `SELECT FOR UPDATE`           | Database (SQL) | Row-level pessimistic lock                |
| `@Lock(PESSIMISTIC_WRITE)`   | JPA annotation | ORM-level pessimistic locking             |

The **strategy** is always the same:
1. **Acquire lock** (before accessing data)
2. **Read and/or modify** data (while holding lock)
3. **Release lock** (after operation is complete)

---

## 3. Java Implementations of Pessimistic Locking

### 3.1 synchronized Keyword

The simplest form of pessimistic locking in Java:

```java
public class BankAccount {
    private double balance;

    public synchronized void withdraw(double amount) {
        // Lock acquired automatically (monitor lock)
        if (balance >= amount) {
            balance -= amount;
        }
        // Lock released automatically when method exits
    }

    public synchronized void deposit(double amount) {
        balance += amount;
    }
}
```

With `synchronized`, the lock is the **object's intrinsic monitor**. Only one thread can
execute any `synchronized` method on the same object at a time.

### 3.2 ReentrantLock

More flexible pessimistic locking with explicit control:

```java
public class BankAccount {
    private double balance;
    private final ReentrantLock lock = new ReentrantLock();

    public void withdraw(double amount) {
        lock.lock();          // Pessimistic: lock BEFORE checking/modifying
        try {
            if (balance >= amount) {
                balance -= amount;
            }
        } finally {
            lock.unlock();    // Always release in finally
        }
    }
}
```

Advantages over `synchronized`:
- `tryLock()` — non-blocking attempt
- `tryLock(timeout)` — timed attempt
- `lockInterruptibly()` — interruptible
- Fair locking option
- Multiple `Condition` objects

### 3.3 ReentrantReadWriteLock (Write Lock)

The **write lock** of a `ReentrantReadWriteLock` is pessimistic — it blocks ALL other
readers and writers:

```java
public class Cache {
    private final Map<String, String> data = new HashMap<>();
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    public void put(String key, String value) {
        rwLock.writeLock().lock();      // Pessimistic: exclusive access
        try {
            data.put(key, value);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public String get(String key) {
        rwLock.readLock().lock();       // Shared access (multiple readers OK)
        try {
            return data.get(key);
        } finally {
            rwLock.readLock().unlock();
        }
    }
}
```

### 3.4 Database-Level: SELECT FOR UPDATE

In JDBC/SQL, pessimistic locking at the database level:

```java
Connection conn = dataSource.getConnection();
conn.setAutoCommit(false);

PreparedStatement stmt = conn.prepareStatement(
    "SELECT balance FROM accounts WHERE id = ? FOR UPDATE"
);
stmt.setInt(1, accountId);
ResultSet rs = stmt.executeQuery();
// Row is now LOCKED — no other transaction can modify it

if (rs.next()) {
    double balance = rs.getDouble("balance");
    if (balance >= amount) {
        PreparedStatement update = conn.prepareStatement(
            "UPDATE accounts SET balance = balance - ? WHERE id = ?"
        );
        update.setDouble(1, amount);
        update.setInt(2, accountId);
        update.executeUpdate();
    }
}

conn.commit();  // Lock is released on commit
```

---

## 4. How Pessimistic Locking Works — Step by Step

### The Fundamental Pattern

```
Thread-A wants to modify shared resource R:

Step 1: ACQUIRE LOCK on R
        ┌─────────────────────────────────────┐
        │  If lock is free → acquire it        │
        │  If lock is held → BLOCK and wait    │
        └─────────────────────────────────────┘

Step 2: READ data from R
        (safe — no one else can modify R)

Step 3: MODIFY data in R
        (safe — exclusive access guaranteed)

Step 4: RELEASE LOCK on R
        ┌─────────────────────────────────────┐
        │  Other waiting threads can now       │
        │  compete for the lock                │
        └─────────────────────────────────────┘
```

### Execution Timeline with Multiple Threads

```
Thread-A:  [acquire lock]──[read]──[modify]──[release lock]
Thread-B:  ──────[try acquire]──[BLOCKED]────────────────────[acquire]──[read]──[modify]──[release]
Thread-C:  ────────[try acquire]──[BLOCKED]──────────────────────────[BLOCKED]─────────────────────[acquire]──...

Thread-B and Thread-C are forced to wait. No concurrent access occurs.
This is safe but potentially slow under high contention.
```

### Contrast with Optimistic Approach

```
PESSIMISTIC (lock first, then access):
  lock() → read → modify → unlock()
  Guarantee: no conflicts possible

OPTIMISTIC (access first, check later):
  read + version → modify → check version → commit OR retry
  Guarantee: conflicts detected after the fact
```

---

## 5. Pessimistic vs Optimistic Locking — Detailed Comparison

### Comprehensive Comparison Table

| Aspect                    | Pessimistic Locking                  | Optimistic Locking                    |
|---------------------------|--------------------------------------|---------------------------------------|
| **Philosophy**            | Assume conflict WILL happen          | Assume conflict is UNLIKELY           |
| **Mechanism**             | Lock before access                   | Detect conflict after access          |
| **Blocking**              | Yes — threads block waiting for lock | No — threads never block on a lock    |
| **Conflict handling**     | Prevention (conflicts can't happen)  | Detection + retry                     |
| **Throughput (low contention)** | Lower (lock overhead even when no conflict) | Higher (no lock overhead) |
| **Throughput (high contention)** | Higher (no wasted retry work)  | Lower (many retries, wasted work)    |
| **Starvation risk**       | Possible (with non-fair locks)       | Possible (thread may retry forever)   |
| **Deadlock risk**         | Yes (if multiple locks involved)     | No (no locks to deadlock on)          |
| **Livelock risk**         | No                                   | Yes (threads may retry forever)       |
| **Memory overhead**       | Lock object + wait queue             | Version counter only                  |
| **Best for reads**        | Acceptable but overkill              | Excellent (reads never conflict)      |
| **Best for writes**       | Good (guaranteed exclusive access)   | Poor under high write contention      |
| **Java examples**         | `synchronized`, `ReentrantLock`      | `AtomicInteger`, `StampedLock`        |
| **DB examples**           | `SELECT FOR UPDATE`                  | Version column, `@Version`            |
| **Complexity**            | Simple (lock/unlock)                 | Complex (retry logic, ABA problem)    |

### Decision Matrix

```
                         Low Contention         High Contention
                    ┌────────────────────┬────────────────────┐
  Read-Heavy        │  Optimistic ✓✓✓    │  Optimistic ✓✓     │
                    │  (no conflicts)    │  (few write conflicts)│
                    ├────────────────────┼────────────────────┤
  Write-Heavy       │  Either works      │  Pessimistic ✓✓✓   │
                    │  (low conflict)    │  (retries too costly)│
                    ├────────────────────┼────────────────────┤
  Mixed Read/Write  │  Optimistic ✓✓     │  Pessimistic ✓✓    │
                    │  (mostly reads)    │  (frequent conflicts)│
                    └────────────────────┴────────────────────┘
```

### The Fundamental Tradeoff

```
Pessimistic: pays the cost of locking ALWAYS (even when no conflict)
             but never wastes work on failed operations.

Optimistic:  pays ZERO cost when no conflict,
             but pays heavily when conflicts occur (retry all the work).

→ If conflicts are rare:   optimistic wins (rarely pays the retry cost)
→ If conflicts are common: pessimistic wins (retries are more expensive than locks)
```

---

## 6. When to Use Pessimistic Locking

### Use Pessimistic Locking When:

**1. High contention (many writers competing for the same resource)**
```
10 threads writing to the same bank account simultaneously.
With optimistic locking: 9 out of 10 would fail and retry → wasted work.
With pessimistic locking: threads queue up, each succeeds on first try.
```

**2. Critical sections are short**
```
lock();
counter++;     // nanoseconds of work
unlock();
// Lock overhead is proportionally small. Pessimistic works well.
```

**3. Conflicts are LIKELY (> 20-30% of operations conflict)**
```
If 50% of operations would conflict, optimistic locking means 50% of work
is wasted. Pessimistic locking ensures zero wasted work.
```

**4. Retry cost is high**
```
If the operation involves expensive computation or I/O (e.g., calling an
external API), retrying is costly. Better to lock and guarantee success.
```

**5. Correctness requires mutual exclusion**
```
Bank transfer: debit account A, credit account B.
These two operations MUST be atomic. A lock guarantees this.
```

**6. Ordering matters**
```
If operations must execute in a specific order (e.g., FIFO), pessimistic
locking with a fair lock guarantees ordering.
```

### Do NOT Use Pessimistic Locking When:

- Contention is low → optimistic is faster
- Workload is read-heavy → readers don't conflict with each other
- You need maximum throughput → lock contention is the bottleneck
- Operations are idempotent → safe to retry, so optimistic is fine

---

## 7. Database Pessimistic Locking

### SELECT FOR UPDATE

The most common database-level pessimistic lock. It locks the selected rows until the
transaction commits or rolls back.

```sql
-- Thread 1: locks the row
BEGIN;
SELECT * FROM inventory WHERE product_id = 42 FOR UPDATE;
-- Row is now LOCKED. Thread 2 cannot modify it.
UPDATE inventory SET quantity = quantity - 1 WHERE product_id = 42;
COMMIT;
-- Lock released.

-- Thread 2: tries to read the same row
BEGIN;
SELECT * FROM inventory WHERE product_id = 42 FOR UPDATE;
-- BLOCKS here until Thread 1 commits or rolls back.
```

### Lock Types in Databases

| Lock Type         | SQL Syntax                    | Behavior                              |
|-------------------|-------------------------------|---------------------------------------|
| Row-level exclusive | `SELECT ... FOR UPDATE`     | Blocks other FOR UPDATE on same rows  |
| Row-level shared  | `SELECT ... FOR SHARE`        | Blocks FOR UPDATE, allows FOR SHARE   |
| Table-level       | `LOCK TABLE ... IN EXCLUSIVE` | Blocks all access to entire table     |
| Advisory lock     | `pg_advisory_lock(id)`        | Application-controlled, not tied to rows |

### SELECT FOR UPDATE Variants

```sql
-- Standard: blocks until lock is available
SELECT * FROM accounts WHERE id = 1 FOR UPDATE;

-- NOWAIT: fails immediately if row is locked (PostgreSQL, Oracle)
SELECT * FROM accounts WHERE id = 1 FOR UPDATE NOWAIT;

-- SKIP LOCKED: skips locked rows, returns only unlocked ones (PostgreSQL 9.5+)
SELECT * FROM tasks WHERE status = 'pending' FOR UPDATE SKIP LOCKED LIMIT 1;
```

`SKIP LOCKED` is extremely useful for **job queues**: multiple workers can claim different
tasks without blocking each other.

### JPA/Hibernate Pessimistic Locking

```java
// Using EntityManager
Account account = entityManager.find(
    Account.class, accountId,
    LockModeType.PESSIMISTIC_WRITE    // generates SELECT ... FOR UPDATE
);

// Using Spring Data JPA
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT a FROM Account a WHERE a.id = :id")
Account findByIdWithLock(@Param("id") Long id);
```

### Lock Modes in JPA

| LockModeType              | SQL Generated           | Use Case                        |
|---------------------------|-------------------------|---------------------------------|
| `PESSIMISTIC_READ`        | `SELECT ... FOR SHARE`  | Allow concurrent reads          |
| `PESSIMISTIC_WRITE`       | `SELECT ... FOR UPDATE` | Exclusive access (most common)  |
| `PESSIMISTIC_FORCE_INCREMENT` | `FOR UPDATE` + version++ | Pessimistic with version bump |
| `OPTIMISTIC`              | Version check at commit | Optimistic strategy             |
| `OPTIMISTIC_FORCE_INCREMENT` | Version++ at commit  | Optimistic with version bump    |

---

## 8. Disadvantages of Pessimistic Locking

### 8.1 Reduced Concurrency

Pessimistic locks serialize access. Only one thread can access the protected resource at a
time. This directly limits parallelism:

```
With pessimistic lock:    [T1 work] → [T2 work] → [T3 work]  (sequential)
Without lock:             [T1 work]
                          [T2 work]   (parallel, but unsafe)
                          [T3 work]

Throughput with lock ≈ throughput_per_thread × 1
Throughput without lock ≈ throughput_per_thread × N (but data corruption risk)
```

### 8.2 Risk of Deadlock

When multiple locks are involved, pessimistic locking introduces deadlock risk:

```
Thread-A: lock(account1) → tries lock(account2)  ← BLOCKED (T-B holds it)
Thread-B: lock(account2) → tries lock(account1)  ← BLOCKED (T-A holds it)
→ DEADLOCK: both threads wait forever.
```

Prevention strategies:
- **Lock ordering**: always acquire locks in the same order
- **tryLock with timeout**: give up after a timeout
- **Deadlock detection**: JVM can detect via `ThreadMXBean`

### 8.3 Overhead Even When No Contention

Even when only one thread accesses the resource, the lock/unlock operations have overhead:
- `synchronized`: ~20-50 ns per lock/unlock (biased locking optimization helps)
- `ReentrantLock`: ~50-100 ns per lock/unlock
- Database `SELECT FOR UPDATE`: ~1-5 ms per query (network + lock manager)

Optimistic approaches (like `AtomicInteger.compareAndSet`) cost only ~5-15 ns.

### 8.4 Priority Inversion

A low-priority thread holding a lock can block a high-priority thread waiting for it.
The high-priority thread is effectively "inverted" to the priority of the lock holder.

### 8.5 Convoying

Threads queue up behind the lock, forming a "convoy." Even after the lock is released,
threads wake up and execute one by one. This is especially problematic with fair locks.

---

## 9. Real-World Examples

### 9.1 Bank Transfer

```java
public void transfer(Account from, Account to, double amount) {
    // Lock both accounts (in consistent order to prevent deadlock)
    Account first = from.getId() < to.getId() ? from : to;
    Account second = from.getId() < to.getId() ? to : from;

    synchronized (first) {
        synchronized (second) {
            if (from.getBalance() >= amount) {
                from.withdraw(amount);
                to.deposit(amount);
            }
        }
    }
}
```

Why pessimistic? Both accounts must be modified atomically. An optimistic approach would
risk one account being debited but the other not credited if a conflict occurs midway.

### 9.2 Inventory Management (E-commerce)

```java
public boolean reserveItem(int productId, int quantity) {
    lock.lock();
    try {
        int available = inventory.get(productId);
        if (available >= quantity) {
            inventory.put(productId, available - quantity);
            return true;   // reserved successfully
        }
        return false;      // out of stock
    } finally {
        lock.unlock();
    }
}
```

Why pessimistic? With flash sales, hundreds of requests arrive simultaneously. Optimistic
locking would cause massive retry storms. Pessimistic guarantees each request succeeds
or fails without wasted work.

### 9.3 Booking Systems (Airlines, Hotels)

```sql
BEGIN;
SELECT seats_available FROM flights WHERE flight_id = 'UA123' FOR UPDATE;
-- If seats_available > 0:
UPDATE flights SET seats_available = seats_available - 1 WHERE flight_id = 'UA123';
INSERT INTO bookings (flight_id, passenger_id) VALUES ('UA123', 'P456');
COMMIT;
```

Why pessimistic? Double-booking is a critical business error. A pessimistic lock on the
flight row ensures that two passengers cannot book the last seat simultaneously.

### 9.4 Print Queue / Resource Pool

```java
public class PrinterPool {
    private final Queue<Printer> available = new LinkedList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition printerAvailable = lock.newCondition();

    public Printer acquire() throws InterruptedException {
        lock.lock();
        try {
            while (available.isEmpty()) {
                printerAvailable.await();
            }
            return available.poll();
        } finally {
            lock.unlock();
        }
    }

    public void release(Printer printer) {
        lock.lock();
        try {
            available.add(printer);
            printerAvailable.signal();
        } finally {
            lock.unlock();
        }
    }
}
```

---

## 10. Interview Questions & Answers

### Q1: What is pessimistic locking and how does it work?

**A:** Pessimistic locking is a concurrency control strategy that assumes conflicts are
likely, so it acquires an exclusive lock **before** accessing shared data. The pattern
is: acquire lock → read data → modify data → release lock. While the lock is held, no
other thread can access the protected resource. In Java, this is implemented with
`synchronized`, `ReentrantLock`, or at the database level with `SELECT FOR UPDATE`.
It's a strategy, not a specific class.

---

### Q2: When would you choose pessimistic over optimistic locking?

**A:** Choose pessimistic locking when: (1) write contention is high (many threads modifying
the same data), making optimistic retries too expensive; (2) the cost of retrying an
operation is high (e.g., it involves I/O or external API calls); (3) correctness requires
atomic multi-step operations (e.g., bank transfers); (4) you need predictable behavior
without retry storms. Choose optimistic when contention is low and reads dominate writes.

---

### Q3: What are the main disadvantages of pessimistic locking?

**A:** (1) **Reduced concurrency** — only one thread can access the resource at a time,
limiting parallelism. (2) **Deadlock risk** — when multiple locks are involved, threads
can deadlock. (3) **Overhead even without contention** — the lock/unlock cycle costs
50-100ns even when no one else is competing. (4) **Priority inversion** — a low-priority
thread holding a lock can block high-priority threads. (5) **Convoy effect** — threads
queue up behind the lock, causing cascading delays.

---

### Q4: How does database pessimistic locking (SELECT FOR UPDATE) work?

**A:** `SELECT FOR UPDATE` acquires an exclusive row-level lock on the selected rows within
a transaction. Other transactions attempting `SELECT FOR UPDATE` or `UPDATE` on the same
rows will block until the locking transaction commits or rolls back. Variants include
`NOWAIT` (fails immediately if locked) and `SKIP LOCKED` (skips locked rows). The lock
is held for the duration of the transaction, so short transactions are preferred.

---

### Q5: How do you prevent deadlocks with pessimistic locking?

**A:** (1) **Lock ordering** — always acquire locks in a consistent global order (e.g., by
resource ID). If transferring between accounts, always lock the lower-ID account first.
(2) **tryLock with timeout** — use `ReentrantLock.tryLock(timeout, unit)` and back off if
the timeout expires. (3) **Reduce lock scope** — hold locks for the minimum time necessary.
(4) **Single lock** — if possible, use one lock instead of multiple. (5) **Deadlock
detection** — JVM's `ThreadMXBean` can detect deadlocked threads for monitoring.

---

### Q6: Compare synchronized and ReentrantLock for pessimistic locking.

**A:** Both implement pessimistic locking. `synchronized` is simpler (no explicit unlock,
no try-finally needed), has JVM-level optimizations (biased locking, lock coarsening), and
is sufficient for most cases. `ReentrantLock` offers tryLock() for non-blocking attempts,
timed locking, interruptible locking, fair ordering, and multiple Condition variables. Use
`synchronized` for simple cases, `ReentrantLock` when you need the advanced features. Both
are reentrant and provide the same memory visibility guarantees.

---

### Q7: In a microservices architecture, how would you implement pessimistic locking?

**A:** In distributed systems, JVM-level locks don't work across processes. Options: (1)
**Database locks** — `SELECT FOR UPDATE` on a shared database row; (2) **Distributed locks**
— Redis (Redlock algorithm), ZooKeeper, or etcd provide distributed locking primitives;
(3) **Database advisory locks** — `pg_advisory_lock()` in PostgreSQL for application-level
locking. The key challenge is handling lock holder failures — use TTL-based locks with
automatic expiration to prevent permanent lock-holding by crashed processes.

---

### Q8: What is the "convoy effect" in pessimistic locking?

**A:** The convoy effect occurs when multiple threads queue up behind a lock, each executing
sequentially. Even if the lock holder finishes quickly, the next thread must be scheduled
(context switch), execute, and release — then the next thread wakes up, and so on. This
creates a "convoy" that moves at the speed of context switching rather than computation.
It's especially pronounced with fair locks that enforce strict ordering. Mitigation
strategies include reducing critical section size, using read-write locks to allow
concurrent reads, or switching to optimistic locking if contention is low.
