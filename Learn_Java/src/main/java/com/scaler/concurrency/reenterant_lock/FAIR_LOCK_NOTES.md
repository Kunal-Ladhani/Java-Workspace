# Fair Lock — Complete Deep Dive for SDE2

## Table of Contents

1. [What is a Fair Lock?](#1-what-is-a-fair-lock)
2. [Fair vs Non-Fair Lock in ReentrantLock](#2-fair-vs-non-fair-lock-in-reentrantlock)
3. [How Fairness Works Internally (AQS)](#3-how-fairness-works-internally-aqs)
4. [Non-Fair "Barging" Explained](#4-non-fair-barging-explained)
5. [Performance Impact of Fair Locks](#5-performance-impact-of-fair-locks)
6. [Why Non-Fair is the Default](#6-why-non-fair-is-the-default)
7. [Starvation in Non-Fair Locks](#7-starvation-in-non-fair-locks)
8. [The tryLock() Fairness Caveat](#8-the-trylock-fairness-caveat)
9. [When to Use Fair Locks](#9-when-to-use-fair-locks)
10. [Fairness in Other JUC Classes](#10-fairness-in-other-juc-classes)
11. [Interview Questions & Answers](#11-interview-questions--answers)

---

## 1. What is a Fair Lock?

A **fair lock** is a mutual exclusion lock that grants access to threads in the **order they
requested it** — first come, first served. If Thread-A requests the lock before Thread-B,
Thread-A is **guaranteed** to acquire it before Thread-B (assuming both are waiting).

### Formal Definition

> A lock is **fair** if and only if the lock acquisition order matches the temporal order in
> which threads requested the lock. Formally, if thread T₁ calls `lock()` before thread T₂
> calls `lock()`, then T₁ will acquire the lock before T₂, every time, without exception.

### Analogy

Think of a **ticket counter at a bank**:
- **Fair lock** = there's a queue. You take a number. People are served in ticket order.
- **Non-fair lock** = there's a crowd. When the counter opens, whoever pushes to the front gets served.

```
FAIR LOCK (FIFO queue):
   Waiting:   [T1] → [T2] → [T3] → [T4]
   Lock releases → T1 gets it (always the oldest waiter)

NON-FAIR LOCK (barging allowed):
   Waiting:   [T1] → [T2] → [T3] → [T4]
   Lock releases → T5 (just arrived!) might steal it before T1
```

---

## 2. Fair vs Non-Fair Lock in ReentrantLock

`ReentrantLock` is the primary fair lock implementation in Java. Fairness is controlled by
the constructor parameter:

```java
// Non-fair lock (DEFAULT) — maximum throughput, no ordering guarantee
ReentrantLock nonFairLock = new ReentrantLock();        // same as new ReentrantLock(false)
ReentrantLock nonFairLock = new ReentrantLock(false);

// Fair lock — FIFO ordering guaranteed, lower throughput
ReentrantLock fairLock = new ReentrantLock(true);
```

### Key Behavioral Differences

| Aspect                  | Fair Lock (`true`)                         | Non-Fair Lock (`false`)                 |
|-------------------------|--------------------------------------------|-----------------------------------------|
| Acquisition order       | Strict FIFO (queue order)                  | No guarantee — barging allowed          |
| Throughput              | Lower (10-100x slower under contention)    | Higher (fewer context switches)         |
| Starvation possible?    | No — every thread eventually gets the lock | Theoretically yes, practically rare     |
| Default?                | No                                         | Yes                                     |
| Context switches        | More (must wake up next-in-queue thread)   | Fewer (running thread can grab lock)    |
| `isFair()` returns      | `true`                                     | `false`                                 |

### How to Check Fairness at Runtime

```java
ReentrantLock lock = new ReentrantLock(true);
System.out.println(lock.isFair());    // true
System.out.println(lock.toString());  // includes [Fair] or [Non-fair]
```

---

## 3. How Fairness Works Internally (AQS)

Both fair and non-fair `ReentrantLock` are built on **AbstractQueuedSynchronizer (AQS)**,
which maintains a **CLH queue** (Craig, Landin, Hagersten) of waiting threads.

### The AQS State Machine

```
AQS internal state:
┌────────────────────────────────────────────┐
│  state = 0  →  lock is FREE               │
│  state = 1  →  lock is HELD (1 acquisition)│
│  state = N  →  lock is HELD (N reentrant)  │
│                                            │
│  CLH Queue (doubly linked list):           │
│  HEAD ←→ [Node T1] ←→ [Node T2] ←→ TAIL  │
└────────────────────────────────────────────┘
```

### Non-Fair Lock: `NonfairSync.lock()`

```
1. Immediately try CAS(state, 0 → 1)     ← "barging" attempt
2. If CAS succeeds → thread owns the lock (skipped the queue!)
3. If CAS fails → call acquire(1):
   a. tryAcquire(1):
      - If state == 0: CAS(0 → 1) — try again (another barge attempt)
      - If currentThread == owner: state++ (reentrant)
      - Else: return false
   b. If tryAcquire fails → add to CLH queue tail → park (sleep)
```

### Fair Lock: `FairSync.lock()`

```
1. Call acquire(1) directly (NO initial barge attempt)
2. tryAcquire(1):
   a. If state == 0:
      ★ CHECK: hasQueuedPredecessors() — are there threads waiting BEFORE me?
      - If YES → return false (don't barge, respect the queue)
      - If NO  → CAS(0 → 1) — safe to acquire
   b. If currentThread == owner: state++ (reentrant)
   c. Else: return false → add to queue → park
```

### The Critical Difference: `hasQueuedPredecessors()`

This is the **single method** that makes a lock fair. It checks:
1. Is there a CLH queue with waiting threads?
2. Is the first waiter in the queue a **different** thread than the current one?

```java
// Simplified pseudocode of hasQueuedPredecessors()
public boolean hasQueuedPredecessors() {
    Node head = this.head;
    Node tail = this.tail;
    if (head == tail) return false;         // queue is empty
    Node firstWaiter = head.next;
    return firstWaiter == null || firstWaiter.thread != Thread.currentThread();
}
```

If `hasQueuedPredecessors()` returns `true`, the fair lock **refuses** to let the current
thread acquire the lock, even if the lock is free. The thread must join the queue and wait
its turn.

---

## 4. Non-Fair "Barging" Explained

**Barging** is when a newly arriving thread acquires the lock **ahead of** threads that have
been waiting longer. This is the default behavior in `ReentrantLock`.

### Why Does Barging Happen?

When a lock is released, there's a brief window:
1. Thread-A releases the lock (`state` goes from 1 → 0)
2. AQS signals the next thread in the queue (Thread-B) to wake up
3. Thread-B is **unparked** but needs time for the OS to actually schedule it
4. During this window, Thread-C (a brand new thread) calls `lock()`
5. Thread-C does a CAS and **steals** the lock before Thread-B even wakes up

```
Timeline:
t=0   Thread-A releases lock. AQS unparks Thread-B.
t=0   Thread-C arrives, tries CAS(0 → 1). SUCCEEDS!  ← BARGE!
t=1   Thread-B wakes up, tries CAS. FAILS. Goes back to sleep.
t=5   Thread-C releases. Thread-B finally gets the lock.
```

### Why is Barging Beneficial?

Thread-C was **already running on a CPU**. Thread-B was **sleeping** (parked). Waking up
Thread-B requires:
- OS scheduler intervention
- Context switch (save Thread-C state → restore Thread-B state)
- Cache warming (Thread-B's working set likely evicted from L1/L2)

By letting Thread-C barge, we **avoid the context switch entirely**. Thread-C does its work
and releases the lock, often before Thread-B even finishes waking up. This is a massive
throughput win under high contention.

---

## 5. Performance Impact of Fair Locks

Fair locks are **significantly slower** than non-fair locks under contention. The primary
cost is **forced context switching**.

### Why Fair Locks Are Slower

1. **No barging** → every lock acquisition forces a context switch to the oldest waiter
2. **Queue maintenance** → must always check `hasQueuedPredecessors()` before CAS
3. **Convoy effect** → threads form a "convoy" behind the lock, each taking turns, each
   requiring an OS context switch

### Benchmark Data (Approximate)

These numbers are from the well-known benchmarks in "Java Concurrency in Practice" by
Brian Goetz and from JMH benchmarks on typical hardware:

| Scenario                          | Non-Fair Throughput | Fair Throughput | Ratio     |
|-----------------------------------|--------------------:|----------------:|----------:|
| 2 threads, light contention       |           ~1.0x     |       ~0.7x     |  ~1.4x    |
| 4 threads, moderate contention    |           ~1.0x     |       ~0.2x     |  ~5x      |
| 8 threads, heavy contention       |           ~1.0x     |       ~0.05x    |  ~20x     |
| 16 threads, very heavy contention |           ~1.0x     |       ~0.01x    |  ~100x    |
| Single thread, no contention      |           ~1.0x     |       ~0.95x    |  ~1.05x   |

### Key Takeaways

- Under **no contention** (single thread), fair and non-fair are nearly identical
- Under **light contention** (2-4 threads), fair is 1.5-5x slower
- Under **heavy contention** (8+ threads), fair can be **10-100x slower**
- The cost is proportional to context switches, which are expensive (~1-10 μs each)

### The Convoy Effect

With a fair lock, threads form an orderly queue. Each acquisition requires waking the
next waiter and putting the previous holder to sleep. This creates a "convoy" pattern:

```
Non-Fair (threads reuse lock while hot):
  T1: ████████░░████████░░████████
  T2: ░░░░░░░░██░░░░░░░░██░░░░░░░░   (barges in during T1's gaps)

Fair (forced round-robin):
  T1: ████░░░░░░░░████░░░░░░░░████
  T2: ░░░░████░░░░░░░░████░░░░░░░░   (must wait turn)
  T3: ░░░░░░░░████░░░░░░░░████░░░░
  (lots of idle time between context switches)
```

---

## 6. Why Non-Fair is the Default

The Java designers made non-fair the default for `ReentrantLock` because:

1. **Throughput matters more than fairness in most applications.** Most locks protect short
   critical sections. Maximizing throughput means more work gets done per second.

2. **Starvation is rare in practice.** While non-fair locks *can* theoretically starve a
   thread, in practice the OS scheduler ensures all threads eventually make progress.

3. **`synchronized` is also non-fair.** Java developers were already used to non-fair
   locking. Making `ReentrantLock` non-fair by default was consistent.

4. **Fair locks have hidden costs.** Beyond raw throughput, fair locks increase latency
   variance (every thread waits the same amount, but overall everything is slower).

### The Principle

> Use non-fair locks unless you have a **specific, measured** reason to need fairness.
> Fairness is a feature you opt into, not a default you opt out of.

---

## 7. Starvation in Non-Fair Locks

### Can Non-Fair Locks Cause Starvation?

**Theoretically: Yes.** If new threads keep arriving and barging in, a waiting thread
could be starved indefinitely.

**Practically: Almost never.** Here's why:

1. **Barging only works when the lock is momentarily free.** If contention is high, barging
   attempts often fail, and threads join the queue anyway.

2. **The OS scheduler is fair-ish.** Even with barging, the OS eventually schedules all
   runnable threads, giving them a chance to barge.

3. **Statistical improbability.** For a thread to be starved, EVERY lock release would need
   to be intercepted by a barging thread. With N waiting threads and random scheduling,
   the probability of K consecutive barges approaches 0 as K grows.

### When Starvation IS a Risk

- **Very high contention** with a mix of fast and slow threads
- **Priority inversion** on the OS level (low-priority thread never gets CPU time to barge)
- **Real-time systems** where worst-case latency matters (use fair locks here)

### Java's AQS Safety Net

Even in non-fair mode, AQS has a built-in mechanism: after a barging attempt fails, the
thread **joins the queue**. Once in the queue, threads are woken in FIFO order. So in
practice, the worst case for any thread is: it missed one barge opportunity, then it's
in the queue and will be served in order.

---

## 8. The tryLock() Fairness Caveat

This is a common interview trick question and a real-world gotcha.

### The Surprise: `tryLock()` Ignores Fairness!

Even on a **fair** `ReentrantLock`, calling the zero-argument `tryLock()` performs a
**non-fair** acquisition attempt. It will barge ahead of waiting threads.

```java
ReentrantLock fairLock = new ReentrantLock(true);

// This DOES NOT respect fairness — it will barge!
boolean acquired = fairLock.tryLock();

// This DOES respect fairness — it checks the queue first
boolean acquired = fairLock.tryLock(0, TimeUnit.SECONDS);
```

### Why?

From the Javadoc of `Lock.tryLock()`:

> "Even when this lock has been set to use a fair ordering policy, a call to tryLock()
> will immediately acquire the lock if it is available, whether or not other threads are
> currently waiting for the lock. This barging behavior can be useful in certain
> circumstances, even though it breaks fairness."

The rationale is:
1. `tryLock()` is meant to be a **non-blocking, opportunistic** attempt
2. If you're calling `tryLock()`, you presumably want to know "is the lock free RIGHT NOW?"
3. Checking the queue would defeat the purpose of a zero-wait, non-blocking try

### The Fix: Use `tryLock(0, TimeUnit.SECONDS)`

If you want `tryLock()` behavior (non-blocking, returns immediately) but **with fairness**:

```java
ReentrantLock fairLock = new ReentrantLock(true);

try {
    // Timed tryLock respects fairness — checks the queue
    boolean acquired = fairLock.tryLock(0, TimeUnit.SECONDS);
    if (acquired) {
        try {
            // critical section
        } finally {
            fairLock.unlock();
        }
    }
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
}
```

### Key Difference

| Method                           | Respects Fairness? | Throws InterruptedException? |
|----------------------------------|--------------------|------------------------------|
| `lock()`                         | Yes                | No                           |
| `lockInterruptibly()`            | Yes                | Yes                          |
| `tryLock()`                      | **NO**             | No                           |
| `tryLock(time, unit)`            | Yes                | Yes                          |
| `tryLock(0, TimeUnit.SECONDS)`   | Yes                | Yes                          |

---

## 9. When to Use Fair Locks

Fair locks are the right choice in specific scenarios:

### Use Fair Locks When:

1. **Starvation is unacceptable.** If every request must be served within a bounded time,
   fair locks guarantee no thread waits indefinitely.

2. **SLA/regulatory compliance.** In financial systems, trading platforms, or telecom
   systems, regulations may require that requests are processed in arrival order.

3. **Latency predictability matters more than throughput.** Fair locks have lower throughput
   but more **predictable** per-thread latency. Every thread waits roughly the same amount.

4. **Reader threads must not starve writers (or vice versa).** Use
   `ReentrantReadWriteLock(true)` to ensure writers aren't indefinitely postponed by readers.

5. **Debugging fairness-related bugs.** If you suspect starvation, switching to a fair lock
   can confirm whether the lock itself is the problem.

### Do NOT Use Fair Locks When:

1. **Throughput is the priority.** Most web servers, batch processors, and data pipelines
   should use non-fair locks.

2. **Contention is low.** If threads rarely compete for the lock, fairness doesn't matter
   and the overhead is wasted.

3. **Critical sections are very short.** Short critical sections mean threads barely wait
   at all, making starvation extremely unlikely.

### Real-World Examples

| System                           | Fair Lock Needed? | Reason                                        |
|----------------------------------|-------------------|-----------------------------------------------|
| Web server request handler       | No                | Throughput matters most                        |
| Stock exchange order matching    | Yes               | Orders MUST be processed in arrival order      |
| Database connection pool         | Maybe             | Prevent long-waiting requests from timing out  |
| Logging framework                | No                | Log order doesn't affect correctness           |
| Medical device control system    | Yes               | Regulatory: all commands processed in order    |
| Thread pool task queue            | Yes (maybe)      | Prevent task starvation                        |

---

## 10. Fairness in Other JUC Classes

Fairness isn't unique to `ReentrantLock`. Several `java.util.concurrent` classes support it:

### Semaphore

```java
// Non-fair semaphore (default)
Semaphore sem = new Semaphore(5);

// Fair semaphore — permits granted in FIFO order
Semaphore fairSem = new Semaphore(5, true);
```

With a fair `Semaphore`, threads acquire permits in the order they called `acquire()`.
Same performance tradeoff applies.

### ReentrantReadWriteLock

```java
// Non-fair (default)
ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

// Fair — readers and writers served in FIFO order
ReentrantReadWriteLock fairRwLock = new ReentrantReadWriteLock(true);
```

In a fair `ReentrantReadWriteLock`:
- A reader arriving behind a waiting writer **cannot** acquire the read lock (prevents
  writer starvation)
- A writer arriving behind a waiting reader **cannot** jump the queue

In a non-fair `ReentrantReadWriteLock`:
- Readers can barge ahead of waiting writers (can cause writer starvation)
- Writers can barge ahead of waiting readers

### ArrayBlockingQueue

```java
// Non-fair (default)
ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<>(100);

// Fair — producers and consumers served in FIFO order
ArrayBlockingQueue<String> fairQueue = new ArrayBlockingQueue<>(100, true);
```

### Summary of Fair-Capable JUC Classes

| Class                        | Constructor for Fairness             | Default |
|------------------------------|--------------------------------------|---------|
| `ReentrantLock`              | `new ReentrantLock(true)`            | false   |
| `Semaphore`                  | `new Semaphore(n, true)`             | false   |
| `ReentrantReadWriteLock`     | `new ReentrantReadWriteLock(true)`   | false   |
| `ArrayBlockingQueue`         | `new ArrayBlockingQueue(n, true)`    | false   |

All of them use AQS internally, and fairness is controlled the same way:
checking `hasQueuedPredecessors()` before attempting CAS.

---

## 11. Interview Questions & Answers

### Q1: What is a fair lock and how does it differ from a non-fair lock?

**A:** A fair lock grants the lock to threads in the order they requested it (FIFO). A
non-fair lock allows "barging" — a newly arriving thread can acquire the lock before
threads that have been waiting longer. In Java, `new ReentrantLock(true)` creates a fair
lock, `new ReentrantLock(false)` (the default) creates a non-fair lock. The key difference
is that the fair lock's `tryAcquire()` calls `hasQueuedPredecessors()` before attempting
CAS, while the non-fair version does not.

---

### Q2: Why is non-fair the default for ReentrantLock?

**A:** Because non-fair locks provide significantly higher throughput. When a lock is
released, a non-fair lock lets the currently-running thread acquire it immediately via CAS,
avoiding the cost of a context switch to wake up the next queued thread. Under heavy
contention, this can be 10-100x faster. Most applications prioritize throughput over
strict ordering, and starvation is extremely rare in practice.

---

### Q3: Explain how AQS enforces fairness in ReentrantLock(true).

**A:** AQS maintains a CLH queue of waiting threads. In `FairSync.tryAcquire()`, before
attempting `compareAndSetState(0, 1)`, it calls `hasQueuedPredecessors()`. This method
checks if there are any threads in the queue that arrived before the current thread.
If yes, `tryAcquire()` returns false, forcing the thread to join the queue tail instead
of barging. This ensures FIFO ordering. The non-fair version skips this check and attempts
CAS immediately.

---

### Q4: Does `tryLock()` respect fairness on a fair ReentrantLock?

**A:** No. The zero-argument `tryLock()` is explicitly documented to be non-fair even on
a fair lock. It performs an immediate CAS without checking the queue. This is by design —
`tryLock()` is meant to be a non-blocking opportunistic attempt. To get fair `tryLock()`
behavior, use `tryLock(0, TimeUnit.SECONDS)`, which does check `hasQueuedPredecessors()`.
Note that the timed version also throws `InterruptedException`, unlike the zero-arg version.

---

### Q5: Can non-fair locks cause thread starvation? How likely is it?

**A:** Theoretically yes — if new threads continuously barge in, a waiting thread could be
delayed indefinitely. In practice, this is extremely rare for several reasons: (1) barging
only succeeds when the lock is momentarily free; under high contention, most barge attempts
fail; (2) the OS scheduler distributes CPU time across threads, ensuring all get chances
to barge; (3) once a thread enters the AQS queue after a failed barge, it's woken in FIFO
order. The probability of infinite starvation approaches zero.

---

### Q6: When should you use a fair lock in production?

**A:** Use fair locks when: (1) latency predictability is more important than throughput
(e.g., real-time systems); (2) regulatory requirements mandate FIFO processing (e.g.,
financial order matching); (3) you've measured starvation occurring with non-fair locks;
(4) preventing starvation is a correctness requirement, not just a performance preference.
Avoid fair locks for high-throughput servers, batch processing, or when critical sections
are very short.

---

### Q7: Explain the "barging" behavior in non-fair ReentrantLock.

**A:** When a non-fair lock is released, AQS sets `state` to 0 and unparks the head of
the queue. However, between setting `state = 0` and the parked thread waking up, a new
thread calling `lock()` can execute `CAS(state, 0, 1)` successfully. This "barges" the
new thread ahead of the queue. The barging thread was already running on a CPU core, so
it avoids the context switch cost that waking the queued thread would require. This is
why non-fair locks have higher throughput.

---

### Q8: What other JUC classes support fairness, and how?

**A:** Several classes support fairness via constructor parameters: `Semaphore(permits, true)`
for fair permit acquisition, `ReentrantReadWriteLock(true)` for fair reader/writer ordering,
and `ArrayBlockingQueue(capacity, true)` for fair producer/consumer access. All use AQS
internally with the same `hasQueuedPredecessors()` check. The tradeoff is always the same:
fairness guarantees FIFO ordering but reduces throughput due to forced context switching.
