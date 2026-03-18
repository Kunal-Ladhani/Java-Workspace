# ReentrantLock - Complete Guide for SDE2

## Table of Contents

1. [What is ReentrantLock?](#1-what-is-reentrantlock)
2. [What Does "Reentrant" Mean?](#2-what-does-reentrant-mean)
3. [ReentrantLock vs synchronized](#3-reentrantlock-vs-synchronized)
4. [How ReentrantLock Works Internally (AQS)](#4-how-reentrantlock-works-internally-aqs)
5. [Fairness: Fair vs Non-Fair Locks](#5-fairness-fair-vs-non-fair-locks)
6. [The Lock API: lock(), tryLock(), lockInterruptibly()](#6-the-lock-api-lock-trylock-lockinterruptibly)
7. [Condition Variables: await() / signal()](#7-condition-variables-await--signal)
8. [Common Patterns and Real-World Use Cases](#8-common-patterns-and-real-world-use-cases)
9. [Deadlock Prevention with tryLock()](#9-deadlock-prevention-with-trylock)
10. [Best Practices and Pitfalls](#10-best-practices-and-pitfalls)
11. [Interview Questions & Answers](#11-interview-questions--answers)

---

## 1. What is ReentrantLock?

`ReentrantLock` is a class in `java.util.concurrent.locks` that implements the `Lock` interface.
It provides the **same mutual exclusion** guarantees as `synchronized`, but with **much more
flexibility and control**.

```java
import java.util.concurrent.locks.ReentrantLock;

Lock lock = new ReentrantLock();        // non-fair (default)
Lock fairLock = new ReentrantLock(true); // fair
```

### Why was it introduced?

`synchronized` has limitations:

- You **can't try** to acquire a lock without blocking forever.
- You **can't interrupt** a thread waiting for a lock.
- You **can't set a timeout** on lock acquisition.
- You **can't have multiple condition queues** (wait-sets) on one lock.
- You **have no control over fairness** (which thread gets the lock next).

`ReentrantLock` was introduced in Java 5 (JDK 1.5) as part of `java.util.concurrent` to
solve all of these limitations.

---

## 2. What Does "Reentrant" Mean?

**Reentrant** = the same thread can acquire the same lock multiple times without deadlocking.

Each time the thread acquires the lock, an internal **hold count** increments by 1.
Each `unlock()` decrements the hold count by 1. The lock is truly released only when the
hold count reaches **0**.

```java
ReentrantLock lock = new ReentrantLock();

lock.lock();    // hold count = 1
lock.lock();    // hold count = 2 (same thread, no deadlock!)
lock.lock();    // hold count = 3

lock.unlock();  // hold count = 2
lock.unlock();  // hold count = 1
lock.unlock();  // hold count = 0 → lock is RELEASED
```

### Why does reentrancy matter?

Without reentrancy, this code would **deadlock**:

```java
public synchronized void methodA() {
    // Already holds the lock
    methodB(); // Tries to acquire the SAME lock → deadlock without reentrancy!
}

public synchronized void methodB() {
    // needs the same monitor lock
}
```

Both `synchronized` and `ReentrantLock` are reentrant. This is critical for recursive
algorithms and calling other synchronized methods from within a synchronized block.

### Maximum Hold Count

A thread can re-acquire the lock up to **2,147,483,647** times (Integer.MAX_VALUE).
If exceeded, it throws an `Error`.

---

## 3. ReentrantLock vs synchronized

This is one of the **most asked interview questions** for SDE2 roles.


| Feature                       | `synchronized`              | `ReentrantLock`                             |
| ----------------------------- | --------------------------- | ------------------------------------------- |
| Type                          | JVM keyword (intrinsic)     | Java class (explicit)                       |
| Lock/Unlock                   | Automatic (scope-based)     | Manual (`lock()` / `unlock()`)              |
| Reentrancy                    | Yes                         | Yes                                         |
| Fairness                      | No control                  | Yes (`new ReentrantLock(true)`)             |
| Try lock (non-blocking)       | Not possible                | `tryLock()`                                 |
| Timed lock                    | Not possible                | `tryLock(time, unit)`                       |
| Interruptible lock            | Not possible                | `lockInterruptibly()`                       |
| Multiple conditions           | No (1 wait-set per monitor) | Yes (`newCondition()`)                      |
| Lock status check             | No                          | `isLocked()`, `isHeldByCurrentThread()`     |
| Lock across methods           | Not possible                | Yes (lock in one method, unlock in another) |
| Performance (low contention)  | ~Same (JVM optimized)       | ~Same                                       |
| Performance (high contention) | Good                        | Better (tryLock avoids blocking)            |
| Risk of misuse                | Low (auto unlock)           | Higher (must unlock in finally!)            |


### The Golden Rule: When to use which?

**Use `synchronized` when:**

- Simple mutual exclusion is enough.
- You want simpler, less error-prone code.
- You don't need tryLock, fairness, or conditions.

**Use `ReentrantLock` when:**

- You need `tryLock()` or timeout-based acquisition.
- You need fairness guarantees.
- You need multiple condition variables (e.g., producer-consumer).
- You need interruptible lock acquisition.
- You need to lock in one method and unlock in another.
- You're doing hand-over-hand locking (e.g., concurrent linked lists).

---

## 4. How ReentrantLock Works Internally (AQS)

> This section is important for SDE2 interviews. Interviewers love asking about internals.

### AbstractQueuedSynchronizer (AQS)

ReentrantLock **delegates all locking to AQS** (AbstractQueuedSynchronizer). AQS is the
backbone of many `java.util.concurrent` classes:

- `ReentrantLock`
- `ReentrantReadWriteLock`
- `Semaphore`
- `CountDownLatch`
- `FutureTask`

### Key Internal Components

```
┌─────────────────────────────────────────────────────┐
│                  ReentrantLock                      │
│                                                     │
│  ┌─────────────────────────────────────────────┐    │
│  │           AQS (Sync subclass)               │    │
│  │                                             │    │
│  │  ┌──────────────────────────────────┐       │    │
│  │  │  volatile int state              │       │    │
│  │  │  0 = unlocked                    │       │    │
│  │  │  1+ = locked (count = reentrancy)│       │    │
│  │  └──────────────────────────────────┘       │    │
│  │                                             │    │
│  │  ┌──────────────────────────────────┐       │    │
│  │  │  Thread exclusiveOwnerThread     │       │    │
│  │  │  (which thread holds the lock)   │       │    │
│  │  └──────────────────────────────────┘       │    │
│  │                                             │    │
│  │  ┌──────────────────────────────────┐       │    │
│  │  │  CLH Queue (FIFO)                │       │    │
│  │  │  HEAD ↔ Node ↔ Node ↔ TAIL       │       │    │
│  │  │  (waiting threads)               │       │    │
│  │  └──────────────────────────────────┘       │    │
│  └─────────────────────────────────────────────┘    │
│                                                     │
│  Two implementations:                               │
│    ├── NonfairSync (default)                        │
│    └── FairSync                                     │
└─────────────────────────────────────────────────────┘
```

### Step-by-Step: What happens when you call `lock()`?

#### Non-Fair Lock (default):

```
Thread-A calls lock():
  1. CAS: try to set state from 0 → 1
     ├── SUCCESS → Thread-A is now the exclusive owner. Done.
     └── FAIL → state is not 0, someone holds the lock
         2. Is the current thread already the owner? (Reentrancy check)
            ├── YES → increment state (1 → 2 → 3...). Done.
            └── NO → 
                3. Create a Node for Thread-A, add to CLH queue tail
                4. Park (block) Thread-A using LockSupport.park()
                5. When unparked, retry from step 1
```

#### Fair Lock:

```
Thread-A calls lock():
  1. Is the CLH queue empty AND state == 0?
     ├── YES → CAS: set state 0 → 1. Thread-A is owner.
     └── NO → Even if state == 0, if there are threads queued ahead:
              Enqueue Thread-A at the tail. Wait your turn.
```

### Step-by-Step: What happens when you call `unlock()`?

```
Thread-A calls unlock():
  1. Decrement state by 1
  2. Is state now 0?
     ├── YES → Set exclusiveOwnerThread to null.
     │         Unpark the FIRST thread in the CLH queue.
     └── NO → Lock is still held (reentrant). Do nothing else.
```

### CAS (Compare-And-Swap)

CAS is a **CPU-level atomic operation** that does:

```
if (current_value == expected) {
    current_value = new_value;
    return true;  // success
} else {
    return false; // someone else changed it
}
```

This happens in a **single CPU instruction** — no locking needed. It's the foundation
of lock-free / wait-free algorithms.

In Java, CAS is exposed through `Unsafe` (internal) and `VarHandle` (public, Java 9+).

### The CLH Queue

CLH stands for Craig, Landin, and Hagersten (the inventors). It's a **FIFO doubly-linked
list** where each node represents a waiting thread.

```
  HEAD ←→ [Thread-B|SIGNAL] ←→ [Thread-C|SIGNAL] ←→ TAIL
              ↑
         First to be unparked when lock is released
```

Node `waitStatus` values:

- `0` — Initial state
- `SIGNAL (-1)` — "My successor needs to be unparked when I release"
- `CANCELLED (1)` — Thread gave up waiting (timeout or interrupt)
- `CONDITION (-2)` — Node is in a condition queue (not the main CLH queue)

---

## 5. Fairness: Fair vs Non-Fair Locks

### Non-Fair (default): `new ReentrantLock()`

A newly arriving thread can **steal** the lock even if other threads have been waiting
longer. This is called **barging**.

```
Queue:  [Thread-B waiting] → [Thread-C waiting]

Thread-D arrives and calls lock():
  → CAS succeeds because the lock was just released!
  → Thread-D gets the lock, even though B and C were waiting.
  → B and C continue to wait. 😤
```

**Why is non-fair the default?**

- **Much higher throughput.** The thread that just became runnable can grab the lock
immediately without the overhead of context-switching to the next queued thread.
- In practice, barging rarely causes starvation because threads interleave naturally.

### Fair: `new ReentrantLock(true)`

Threads acquire the lock in **FIFO order**. No barging allowed.

```
Queue:  [Thread-B waiting] → [Thread-C waiting]

Thread-D arrives and calls lock():
  → Queue is non-empty, so Thread-D MUST enqueue.
  → Thread-B will get the lock next (longest waiting).
```

**Trade-offs of fair locks:**

- **Pro:** Prevents starvation. Predictable behavior.
- **Con:** Significantly lower throughput (measured 10-100x slower under high contention
in benchmarks) because every acquisition requires a context switch.

**Important caveat:** `tryLock()` (without timeout) does **NOT** respect fairness, even
on a fair lock. It will barge. Only `tryLock(timeout, unit)` respects fairness.

---

## 6. The Lock API: lock(), tryLock(), lockInterruptibly()

### 6.1 `lock()`

Acquires the lock. If not available, the thread **blocks indefinitely** until it can.

```java
lock.lock();
try {
    // critical section
} finally {
    lock.unlock();
}
```

### 6.2 `tryLock()` — Non-blocking

Attempts to acquire the lock **immediately**. Returns `true` if successful, `false` otherwise.
The thread **never blocks**.

```java
if (lock.tryLock()) {
    try {
        // critical section
    } finally {
        lock.unlock();
    }
} else {
    // lock not available — do something else
    System.out.println("Could not acquire lock, skipping...");
}
```

### 6.3 `tryLock(long time, TimeUnit unit)` — Timed

Waits up to the specified time. Returns `true` if acquired, `false` if timed out.
Responds to interrupts.

```java
try {
    if (lock.tryLock(2, TimeUnit.SECONDS)) {
        try {
            // critical section
        } finally {
            lock.unlock();
        }
    } else {
        System.out.println("Timed out waiting for lock");
    }
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
}
```

### 6.4 `lockInterruptibly()` — Interruptible

Like `lock()` but the waiting thread can be **interrupted**.

```java
try {
    lock.lockInterruptibly();
    try {
        // critical section
    } finally {
        lock.unlock();
    }
} catch (InterruptedException e) {
    System.out.println("Thread was interrupted while waiting for lock");
    Thread.currentThread().interrupt();
}
```

This is useful for **cancellation**. If a thread is waiting for a lock and you want to
cancel the operation (e.g., user presses cancel, request timeout), you can interrupt
the thread.

### Comparison Table


| Method                | Blocks? | Timeout? | Interruptible? | Use Case                              |
| --------------------- | ------- | -------- | -------------- | ------------------------------------- |
| `lock()`              | Yes     | No       | No             | Default — when you must have the lock |
| `tryLock()`           | No      | No       | No             | Optimistic — skip if busy             |
| `tryLock(t, unit)`    | Yes     | Yes      | Yes            | Bounded wait — timeout if too long    |
| `lockInterruptibly()` | Yes     | No       | Yes            | Cancellable lock acquisition          |


---

## 7. Condition Variables: await() / signal()

This is one of the **most powerful** features of ReentrantLock that `synchronized` cannot match.

### What are Condition variables?

With `synchronized`, you have `wait()` and `notify()`. But there's only **one wait-set**
per monitor. All threads waiting on the same object share the same queue.

With `ReentrantLock`, you can create **multiple Condition objects**, each with its own
wait-set. This allows you to wake up **only the right threads**.

### Mapping from synchronized to ReentrantLock


| `synchronized` (Object monitor) | `ReentrantLock` + `Condition` |
| ------------------------------- | ----------------------------- |
| `synchronized(obj)`             | `lock.lock()`                 |
| `obj.wait()`                    | `condition.await()`           |
| `obj.notify()`                  | `condition.signal()`          |
| `obj.notifyAll()`               | `condition.signalAll()`       |
| 1 wait-set per object           | Multiple Conditions per lock  |


### How it works

```java
ReentrantLock lock = new ReentrantLock();
Condition notFull  = lock.newCondition();  // condition 1
Condition notEmpty = lock.newCondition();  // condition 2
```

When a thread calls `condition.await()`:

1. The thread **atomically releases** the associated lock.
2. The thread is **moved to the condition's wait queue** (separate from the CLH queue).
3. The thread **blocks** until signaled or interrupted.
4. When signaled, the thread is **moved back to the CLH queue** to re-acquire the lock.
5. `await()` **returns only after re-acquiring the lock**.

### Classic Example: Bounded Buffer (Producer-Consumer)

```java
class BoundedBuffer<E> {
    final Lock lock = new ReentrantLock();
    final Condition notFull  = lock.newCondition();
    final Condition notEmpty = lock.newCondition();
    
    final Object[] items;
    int putIndex, takeIndex, count;
    
    public BoundedBuffer(int capacity) {
        items = new Object[capacity];
    }

    public void put(E item) throws InterruptedException {
        lock.lock();
        try {
            while (count == items.length)   // buffer full — wait
                notFull.await();
            
            items[putIndex] = item;
            if (++putIndex == items.length) putIndex = 0;
            count++;
            notEmpty.signal();              // wake a consumer
        } finally {
            lock.unlock();
        }
    }

    public E take() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0)              // buffer empty — wait
                notEmpty.await();
            
            @SuppressWarnings("unchecked")
            E item = (E) items[takeIndex];
            if (++takeIndex == items.length) takeIndex = 0;
            count--;
            notFull.signal();               // wake a producer
            return item;
        } finally {
            lock.unlock();
        }
    }
}
```

Why two conditions?

- `notFull`: Producers wait here when buffer is full. Consumers signal here after taking.
- `notEmpty`: Consumers wait here when buffer is empty. Producers signal here after putting.

With `synchronized`, `notifyAll()` would wake ALL waiting threads (both producers AND
consumers), even when only one type needs to wake up. This is wasteful. With separate
conditions, you wake **only the relevant threads**.

### Spurious Wakeups

Always use `await()` in a **while loop**, not an if statement:

```java
// CORRECT ✅
while (count == 0)
    notEmpty.await();

// WRONG ❌
if (count == 0)
    notEmpty.await();
```

Threads can wake up without being signaled (spurious wakeup). The while loop re-checks
the condition and goes back to waiting if it's still not satisfied.

---

## 8. Common Patterns and Real-World Use Cases

### Pattern 1: Simple Mutual Exclusion

```java
private final ReentrantLock lock = new ReentrantLock();

public void updateCounter() {
    lock.lock();
    try {
        counter++;
    } finally {
        lock.unlock();
    }
}
```

### Pattern 2: Try-Lock with Fallback

```java
public void processWithFallback() {
    if (lock.tryLock()) {
        try {
            processExclusive();
        } finally {
            lock.unlock();
        }
    } else {
        processFallback();
    }
}
```

### Pattern 3: Hand-Over-Hand Locking (Concurrent Linked List)

Used for fine-grained locking on data structures:

```java
// Traversing a linked list with per-node locks
nodeLockA.lock();
try {
    nodeLockB.lock();  // lock next node
    nodeLockA.unlock(); // release previous node
    // now only holding B
    nodeLockC.lock();  // lock next node
    nodeLockB.unlock(); // release previous node
    // ...
} finally { ... }
```

### Pattern 4: Lock Across Methods

Something `synchronized` cannot do:

```java
class ResourceManager {
    private final ReentrantLock lock = new ReentrantLock();
    
    public void startTransaction() {
        lock.lock(); // locked in this method
    }
    
    public void commitTransaction() {
        try {
            // do commit work
        } finally {
            lock.unlock(); // unlocked in a different method!
        }
    }
}
```

### Real-World Use Cases

- **Connection pools** — `tryLock(timeout)` to avoid waiting forever for a connection.
- **Cache updates** — Fair locks ensure all cache refreshes eventually complete.
- **Rate limiters** — `tryLock()` to reject requests when limit is reached.
- **Database transactions** — Lock across methods (begin/commit/rollback).
- **Thread-safe collections** — `ConcurrentHashMap` segments used ReentrantLock internally
(pre-Java 8).

---

## 9. Deadlock Prevention with tryLock()

One of the biggest advantages of `ReentrantLock` is the ability to **avoid deadlocks**
using `tryLock()`.

### The Classic Deadlock

```
Thread-1: locks A, then tries to lock B
Thread-2: locks B, then tries to lock A
→ DEADLOCK! Both threads wait forever.
```

### Solution with tryLock()

```java
public boolean transferMoney(Account from, Account to, int amount) {
    while (true) {
        if (from.lock.tryLock()) {
            try {
                if (to.lock.tryLock()) {
                    try {
                        from.debit(amount);
                        to.credit(amount);
                        return true;
                    } finally {
                        to.lock.unlock();
                    }
                }
            } finally {
                from.lock.unlock();
            }
        }
        // Back off and retry
        Thread.sleep(random.nextInt(100));
    }
}
```

If we can't get both locks, we **release everything and retry**. No deadlock possible.

### Solution with tryLock(timeout)

```java
public boolean transferMoney(Account from, Account to, int amount) 
        throws InterruptedException {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
    
    while (true) {
        if (from.lock.tryLock()) {
            try {
                long remaining = deadline - System.nanoTime();
                if (to.lock.tryLock(remaining, TimeUnit.NANOSECONDS)) {
                    try {
                        from.debit(amount);
                        to.credit(amount);
                        return true;
                    } finally {
                        to.lock.unlock();
                    }
                }
            } finally {
                from.lock.unlock();
            }
        }
        if (System.nanoTime() >= deadline) return false; // give up
        Thread.sleep(random.nextInt(100));
    }
}
```

---

## 10. Best Practices and Pitfalls

### Best Practices

#### 1. ALWAYS unlock in finally

```java
// CORRECT ✅
lock.lock();
try {
    doWork();
} finally {
    lock.unlock(); // guaranteed even if doWork() throws
}

// WRONG ❌
lock.lock();
doWork();      // if this throws, lock is NEVER released → threads starve
lock.unlock();
```

#### 2. Call lock() BEFORE the try block

```java
// CORRECT ✅
lock.lock();
try { ... } finally { lock.unlock(); }

// WRONG ❌
try {
    lock.lock();
    ...
} finally {
    lock.unlock(); // if lock() itself throws, unlock() on un-held lock → IllegalMonitorStateException
}
```

The reasoning: if `lock()` fails (throws), you'd call `unlock()` in finally on a lock
you don't hold → `IllegalMonitorStateException`.

#### 3. Keep critical sections short

The longer you hold the lock, the more contention you create.

```java
// GOOD ✅ — only lock what needs protection
data = prepareData();        // outside lock
lock.lock();
try {
    sharedState = data;      // only the shared mutation
} finally {
    lock.unlock();
}
notify(data);                // outside lock

// BAD ❌ — locking too broadly
lock.lock();
try {
    data = prepareData();    // this doesn't need the lock!
    sharedState = data;
    notify(data);            // this doesn't need the lock!
} finally {
    lock.unlock();
}
```

#### 4. Prefer `synchronized` for simple cases

If you don't need the advanced features of ReentrantLock, `synchronized` is simpler and
less error-prone (automatic unlock).

#### 5. Declare locks as `private final`

```java
private final ReentrantLock lock = new ReentrantLock();
```

- `private` — prevent external code from locking your lock.
- `final` — prevent accidental reassignment.

### Common Pitfalls


| Pitfall                             | Problem                        | Solution                                 |
| ----------------------------------- | ------------------------------ | ---------------------------------------- |
| Forgetting `unlock()`               | Thread starvation, deadlock    | Always use try-finally                   |
| Unlocking someone else's lock       | `IllegalMonitorStateException` | Use `isHeldByCurrentThread()`            |
| Using `static` lock unintentionally | All instances share one lock   | Make lock instance-level unless intended |
| Lock with `==` comparison           | Locks don't use equality       | Each lock instance is unique             |
| Forgetting while loop with await()  | Spurious wakeup bugs           | Always `while(condition) await()`        |


---

## 11. Interview Questions & Answers

### Q1: What is the difference between `ReentrantLock` and `synchronized`?

**A:** See the comparison table in Section 3. Key points: ReentrantLock offers tryLock,
fairness, multiple conditions, and interruptible lock acquisition.

### Q2: What does "reentrant" mean? Why is it important?

**A:** A reentrant lock can be re-acquired by the same thread that already holds it. 
Each acquisition increments the hold count, each release decrements it. It's important
because without reentrancy, calling a synchronized method from within another synchronized
method (on the same monitor) would deadlock.

### Q3: How does ReentrantLock work internally?

**A:** It uses AQS (AbstractQueuedSynchronizer). AQS maintains a volatile int `state`
(0 = free, 1+ = held with count), an `exclusiveOwnerThread`, and a CLH FIFO queue.
Lock acquisition uses CAS to atomically set state. Failed acquirers are enqueued and
parked using `LockSupport.park()`. On unlock, state is decremented and the head of
the queue is unparked.

### Q4: What is the difference between fair and non-fair locks?

**A:** Non-fair (default) allows barging — a newly arriving thread can steal the lock
from waiting threads, giving higher throughput. Fair locks enforce FIFO ordering,
preventing starvation but at significant throughput cost (10-100x slower under contention).

### Q5: How do you prevent deadlock using ReentrantLock?

**A:** Use `tryLock()` or `tryLock(timeout, unit)`. If you can't acquire all needed
locks, release the ones you've acquired and retry. This breaks the "hold and wait"
condition of deadlock.

### Q6: What is the difference between `Condition.await()` and `Object.wait()`?

**A:** `await()` works with `ReentrantLock` and allows **multiple condition queues**
per lock (separate wait-sets for different conditions). `wait()` works with `synchronized`
and has only **one wait-set** per monitor. Multiple conditions enable more precise
signaling (e.g., wake only producers, not consumers).

### Q7: Can `tryLock()` cause fairness violation?

**A:** Yes! The zero-argument `tryLock()` does not honor fairness even on a fair lock.
It will barge if the lock is available. Use `tryLock(0, TimeUnit.SECONDS)` if you want
fair tryLock behavior.

### Q8: What happens if you call `unlock()` without holding the lock?

**A:** Throws `IllegalMonitorStateException`.

### Q9: Is ReentrantLock always better than synchronized?

**A:** No. For simple cases, `synchronized` is preferred because it's simpler, less 
error-prone (auto unlock), and modern JVMs optimize it aggressively (biased locking,
lock elision, lock coarsening). Use ReentrantLock only when you need its advanced features.

### Q10: What is lock coarsening and lock elision?

**A:** JVM optimizations for `synchronized`:

- **Lock coarsening:** If the JVM sees consecutive lock/unlock on the same object, it
merges them into one larger locked region.
- **Lock elision:** If the JVM proves (via escape analysis) that an object is
thread-local, it removes the synchronization entirely.
These optimizations apply to `synchronized` but **not** to `ReentrantLock`.

---

## Quick Reference Cheat Sheet

```java
// Create
ReentrantLock lock = new ReentrantLock();         // non-fair
ReentrantLock fairLock = new ReentrantLock(true);  // fair

// Basic lock
lock.lock();
try { /* critical section */ } finally { lock.unlock(); }

// Try lock
if (lock.tryLock()) { try { /* ... */ } finally { lock.unlock(); } }

// Timed lock
if (lock.tryLock(5, TimeUnit.SECONDS)) { try { /* ... */ } finally { lock.unlock(); } }

// Interruptible
lock.lockInterruptibly();
try { /* ... */ } finally { lock.unlock(); }

// Condition
Condition cond = lock.newCondition();
// waiting thread:  while (!ready) cond.await();
// signaling thread: ready = true; cond.signal();

// Utility methods
lock.isLocked();                 // is any thread holding it?
lock.isHeldByCurrentThread();    // does the current thread hold it?
lock.getHoldCount();             // how many times current thread locked
lock.getQueueLength();           // approx. threads waiting to acquire
lock.hasQueuedThreads();         // are any threads waiting?
lock.isFair();                   // was it created with fairness?
```

