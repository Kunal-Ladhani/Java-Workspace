# Deadlock Deep Dive — SDE2 Interview Preparation

## Table of Contents
1. [What is Deadlock](#1-what-is-deadlock)
2. [Coffman's Four Necessary Conditions](#2-coffmans-four-necessary-conditions)
3. [How to Detect Deadlocks](#3-how-to-detect-deadlocks)
4. [How to Prevent Deadlocks](#4-how-to-prevent-deadlocks)
5. [Livelock vs Deadlock vs Starvation](#5-livelock-vs-deadlock-vs-starvation)
6. [Deadlock in Database Systems](#6-deadlock-in-database-systems)
7. [The Dining Philosophers Problem](#7-the-dining-philosophers-problem)
8. [Java's ThreadMXBean Deadlock Detection API](#8-javas-threadmxbean-deadlock-detection-api)
9. [Real-World Examples](#9-real-world-examples)
10. [SDE2 Interview Q&A](#10-sde2-interview-qa)

---

## 1. What is Deadlock

### Formal Definition

A **deadlock** is a situation where two or more threads are **permanently blocked**, each
waiting to acquire a lock held by another thread in the set. No thread can make progress
because each one is waiting for a resource that will never be released.

### Minimal Example

```
Thread A holds Lock 1, waits for Lock 2.
Thread B holds Lock 2, waits for Lock 1.
```

Neither can proceed. Both are stuck forever (unless killed).

### Visualization (Resource Allocation Graph)

```
Thread A ──(holds)──> Lock 1
Thread A ──(wants)──> Lock 2
Thread B ──(holds)──> Lock 2
Thread B ──(wants)──> Lock 1

Cycle: A → Lock2 → B → Lock1 → A   ← DEADLOCK
```

A deadlock exists if and only if the resource allocation graph contains a **cycle**.

### Why Deadlocks Are Dangerous

- The application hangs silently — no exception, no crash, no log entry.
- Threads are stuck in `BLOCKED` state and never recover.
- Resources (connections, memory, file handles) held by deadlocked threads are leaked.
- In production, this often manifests as "the system stopped responding" with no obvious cause.

---

## 2. Coffman's Four Necessary Conditions

Edward Coffman (1971) identified four conditions that must **all** hold simultaneously for
a deadlock to occur. If you can break **any one**, deadlock is impossible.

### 2.1 Mutual Exclusion

The resource can only be held by one thread at a time. If resources are sharable (e.g.,
read-only data), mutual exclusion doesn't apply and deadlock can't occur.

**How to break**: Use resources that don't require exclusive access (read-write locks allow
concurrent readers, lock-free data structures eliminate mutual exclusion).

### 2.2 Hold and Wait

A thread holds at least one resource and is waiting to acquire additional resources held
by other threads.

**How to break**:
- Acquire all needed locks at once (atomic acquisition) — but this reduces concurrency.
- Use `tryLock()` with a timeout — if you can't get the second lock, release the first.

### 2.3 No Preemption

Resources cannot be forcibly taken from threads that hold them. A thread must voluntarily
release its resources.

**How to break**:
- Use `tryLock()` — if acquisition fails, the thread releases its held locks and retries.
- Use lock timeouts.
- Use interruptible locks (`lockInterruptibly()`).

### 2.4 Circular Wait

There exists a circular chain of threads, each waiting for a resource held by the next
thread in the chain.

**How to break**:
- **Lock ordering**: Impose a total order on all locks and require threads to acquire
  locks in that order. If Thread A must acquire Lock 1 before Lock 2, and Thread B must
  also acquire Lock 1 before Lock 2, no cycle can form.
- This is the **most commonly used** prevention strategy.

### Summary Table

| Condition         | What it means                          | How to break it                     |
|-------------------|----------------------------------------|-------------------------------------|
| Mutual Exclusion  | Only one thread can hold the resource  | Use sharable/lock-free resources    |
| Hold and Wait     | Hold one, wait for another             | tryLock with release, all-or-nothing|
| No Preemption     | Can't forcibly take held resources     | tryLock, timeout, interruptible     |
| Circular Wait     | Cycle in the wait-for graph            | Lock ordering (total order)         |

---

## 3. How to Detect Deadlocks

### 3.1 `jstack` (Command-Line Tool)

`jstack <pid>` dumps the thread stacks of a running JVM and **automatically detects
deadlocks**:

```
$ jstack 12345

Found one Java-level deadlock:
=============================
"Thread-1":
  waiting to lock monitor 0x00007f8b4c003bf8 (object 0x0000000780a0c390, a java.lang.Object),
  which is held by "Thread-0"
"Thread-0":
  waiting to lock monitor 0x00007f8b4c003c58 (object 0x0000000780a0c3a0, a java.lang.Object),
  which is held by "Thread-1"

Java stack information for the threads listed above:
===================================================
"Thread-1":
    at com.example.DeadlockDemo.lambda$main$1(DeadlockDemo.java:25)
    - waiting to lock <0x0000000780a0c390> (a java.lang.Object)
    - locked <0x0000000780a0c3a0> (a java.lang.Object)
"Thread-0":
    at com.example.DeadlockDemo.lambda$main$0(DeadlockDemo.java:15)
    - waiting to lock <0x0000000780a0c3a0> (a java.lang.Object)
    - locked <0x0000000780a0c390> (a java.lang.Object)
```

### 3.2 `ThreadMXBean` (Programmatic Detection)

```java
ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();
long[] deadlockedThreadIds = mxBean.findDeadlockedThreads();

if (deadlockedThreadIds != null) {
    ThreadInfo[] threadInfos = mxBean.getThreadInfo(deadlockedThreadIds, true, true);
    for (ThreadInfo info : threadInfos) {
        System.out.println(info);
    }
}
```

- `findDeadlockedThreads()`: Detects deadlocks involving **both** intrinsic monitors and
  `java.util.concurrent` locks (ReentrantLock, etc.).
- `findMonitorDeadlockedThreads()`: Detects deadlocks involving **only** intrinsic monitors
  (`synchronized`).

### 3.3 VisualVM / JConsole

VisualVM's "Threads" tab has a "Detect Deadlock" button that invokes `ThreadMXBean` under
the hood. JConsole also has this feature.

### 3.4 Java Flight Recorder (JFR)

JFR records lock contention events. Analyzing the recordings in JDK Mission Control can
reveal lock chains that form cycles.

### 3.5 Thread Dump Analysis Tools

- **fastThread.io** — upload thread dumps, get deadlock analysis.
- **TDA (Thread Dump Analyzer)** — open-source tool for analyzing thread dumps.

---

## 4. How to Prevent Deadlocks

### 4.1 Lock Ordering (Break Circular Wait)

The most practical and widely-used strategy. Assign a global order to all lockable resources
and always acquire them in that order.

```java
// Instead of:
// Thread A: lock(a), lock(b)
// Thread B: lock(b), lock(a)

// Enforce consistent order (by identity hash, or by explicit order):
Object first = System.identityHashCode(a) < System.identityHashCode(b) ? a : b;
Object second = (first == a) ? b : a;

synchronized (first) {
    synchronized (second) {
        // safe — no cycle possible
    }
}
```

**Trade-off**: Requires discipline. Every developer must know and follow the ordering.
Difficult to enforce in large codebases.

### 4.2 `tryLock` with Timeout (Break Hold and Wait)

```java
ReentrantLock lock1 = new ReentrantLock();
ReentrantLock lock2 = new ReentrantLock();

boolean acquired = false;
while (!acquired) {
    if (lock1.tryLock(100, TimeUnit.MILLISECONDS)) {
        try {
            if (lock2.tryLock(100, TimeUnit.MILLISECONDS)) {
                try {
                    // Got both locks — do work
                    acquired = true;
                } finally {
                    lock2.unlock();
                }
            }
        } finally {
            if (!acquired) lock1.unlock();  // release first lock if second failed
        }
    }
    if (!acquired) Thread.sleep(50);  // back off and retry
}
```

If a thread can't acquire both locks within the timeout, it releases whatever it holds
and retries. This breaks the "hold and wait" condition.

**Trade-off**: More complex code; risk of livelock if all threads retry at the same time
(mitigate with random backoff).

### 4.3 Lock-Free Algorithms (Break Mutual Exclusion)

Use `AtomicReference`, `AtomicInteger`, CAS operations, and lock-free data structures.
If no lock is ever held, deadlock is impossible.

```java
AtomicReference<Node> head = new AtomicReference<>();

// Lock-free push onto a stack
Node newNode = new Node(value);
Node oldHead;
do {
    oldHead = head.get();
    newNode.next = oldHead;
} while (!head.compareAndSet(oldHead, newNode));
```

**Trade-off**: Much harder to implement correctly. Only practical for specific patterns.

### 4.4 Single Lock (Coarse-Grained)

If all threads share a single lock, there can be no cycle. But this eliminates concurrency.

### 4.5 Avoid Nested Locking

If you never hold more than one lock at a time, deadlock is impossible. Restructure code
to release the first lock before acquiring the second.

---

## 5. Livelock vs Deadlock vs Starvation

### 5.1 Deadlock

Threads are **stuck** — they don't execute any instructions.
State: `BLOCKED` (waiting on monitor) or `WAITING` (waiting on lock/condition).
CPU usage: **zero** (threads are parked).

### 5.2 Livelock

Threads are **active** (executing instructions) but making **no progress**. They keep
responding to each other's actions in a way that prevents any thread from completing.

Analogy: Two people meet in a hallway. Both step left. Both step right. Both step left.
Neither passes.

```java
// Livelock: two threads keep yielding to each other
while (true) {
    if (otherThreadWantsResource) {
        releaseMyResource();  // "You go ahead"
        continue;
    }
    if (acquireResource()) {
        doWork();
        break;
    }
}
```

State: `RUNNABLE` (consuming CPU).
CPU usage: **high** (threads are spinning).

### 5.3 Starvation

A thread is **able** to run but is **never scheduled** because other threads perpetually
monopolize the resource (lock, CPU, etc.).

Causes:
- **Unfair locking**: A high-priority thread repeatedly acquires the lock before
  lower-priority threads.
- **Thread priority abuse**: The OS scheduler favors high-priority threads.
- **Greedy threads**: A thread holds a lock for a very long time.

Fix: Use fair locks (`new ReentrantLock(true)`), reduce lock hold times, avoid thread
priority manipulation.

### Comparison Table

| Aspect         | Deadlock                    | Livelock                    | Starvation                  |
|----------------|-----------------------------|-----------------------------|-----------------------------|
| Threads active?| No (BLOCKED)                | Yes (RUNNABLE)              | Partially (some starved)    |
| Progress?      | None                        | None (busy but useless)     | Some threads progress       |
| CPU usage      | Zero                        | High (spinning)             | Varies                      |
| Detection      | jstack, ThreadMXBean        | Profiler, high CPU + no work| Profiler, fairness analysis |
| Fix            | Lock ordering, tryLock      | Random backoff, limit retries| Fair locks, redesign       |
| Reversible?    | No (without intervention)   | Potentially (with backoff)  | Yes (with fairness)         |

---

## 6. Deadlock in Database Systems

### How DB Deadlocks Happen

Databases use row-level or table-level locks for transactions. Deadlocks occur the same way
as in Java:

```
Transaction A: UPDATE accounts SET balance = balance - 100 WHERE id = 1;  (locks row 1)
Transaction A: UPDATE accounts SET balance = balance + 100 WHERE id = 2;  (needs row 2)

Transaction B: UPDATE accounts SET balance = balance - 50 WHERE id = 2;   (locks row 2)
Transaction B: UPDATE accounts SET balance = balance + 50 WHERE id = 1;   (needs row 1)
```

### How Databases Handle It

1. **Detection**: The DB engine runs a deadlock detector (usually a cycle-detection algorithm
   on the wait-for graph) periodically or on every lock wait.
2. **Resolution**: One transaction is chosen as the **victim** and rolled back. The choice is
   usually based on:
   - Transaction age (newer transactions are cheaper to roll back).
   - Amount of work done (roll back the one with less work).
   - Priority.
3. **Recovery**: The application retries the rolled-back transaction.

### Java Analogy

Database deadlock detection + victim rollback is analogous to using `tryLock()` with timeout:
if you can't get the lock, release everything and retry.

---

## 7. The Dining Philosophers Problem

### Problem Statement (Dijkstra, 1965)

Five philosophers sit at a round table. Between each pair is a fork (5 forks total). A
philosopher alternates between thinking and eating. To eat, they need **both** the fork
on their left and the fork on their right.

### Why It Deadlocks

If all five philosophers simultaneously pick up their left fork, they all wait for their
right fork (held by their neighbor). Circular wait → deadlock.

### Solutions

**1. Lock ordering (asymmetric solution)**:
One philosopher picks up the right fork first instead of the left. This breaks the
circular wait.

**2. Limit concurrency**:
Allow at most N-1 philosophers to eat simultaneously (use a semaphore with N-1 permits).
This ensures at least one philosopher can get both forks.

**3. Try-and-release**:
Each philosopher attempts to pick up both forks. If they can't get the second within a
timeout, they put down the first and retry (with random backoff).

**4. Waiter/arbitrator**:
A central waiter (lock) grants permission to eat. Only one philosopher eats at a time.
Simple but reduces concurrency.

### The Deeper Lesson

The Dining Philosophers problem teaches that deadlock is an emergent property of the
system — each individual philosopher's behavior is perfectly reasonable (pick up left fork,
then right fork), but the collective behavior leads to deadlock.

---

## 8. Java's ThreadMXBean Deadlock Detection API

### Overview

`java.lang.management.ThreadMXBean` provides programmatic deadlock detection:

```java
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

ThreadMXBean bean = ManagementFactory.getThreadMXBean();
```

### Key Methods

| Method                            | Detects                                        |
|-----------------------------------|------------------------------------------------|
| `findDeadlockedThreads()`         | Deadlocks on intrinsic monitors AND j.u.c locks|
| `findMonitorDeadlockedThreads()`  | Deadlocks on intrinsic monitors only           |
| `getThreadInfo(long[], boolean, boolean)` | Full thread info with lock/stack details |

### Usage Pattern (Watchdog Thread)

```java
ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor();
watchdog.scheduleAtFixedRate(() -> {
    long[] ids = bean.findDeadlockedThreads();
    if (ids != null) {
        ThreadInfo[] infos = bean.getThreadInfo(ids, true, true);
        System.err.println("DEADLOCK DETECTED!");
        for (ThreadInfo info : infos) {
            System.err.println(info);
        }
        // Alert, log, or take corrective action
    }
}, 0, 5, TimeUnit.SECONDS);
```

### What `ThreadInfo` Contains

- Thread name, ID, state.
- The lock the thread is waiting on (if any).
- The lock owner (thread that holds the lock).
- Full stack trace.
- List of monitors and synchronizers held.

This information is sufficient to reconstruct the wait-for graph and identify the cycle.

---

## 9. Real-World Examples

### 9.1 Database Connection Pool Deadlock

```java
// Thread A (inside transaction on Connection 1):
Connection conn2 = pool.getConnection();  // blocks if pool is exhausted

// Thread B (inside transaction on Connection 2):
Connection conn1 = pool.getConnection();  // blocks if pool is exhausted
```

If the pool has only 2 connections, and both threads hold one and need another, deadlock.
Fix: Ensure threads never need more than one connection at a time, or increase pool size.

### 9.2 Nested Synchronized Blocks (Classic)

```java
public void transferMoney(Account from, Account to, int amount) {
    synchronized (from) {
        synchronized (to) {
            from.debit(amount);
            to.credit(amount);
        }
    }
}

// Thread A: transferMoney(accountA, accountB, 100);  — locks A, then B
// Thread B: transferMoney(accountB, accountA, 50);   — locks B, then A → DEADLOCK
```

Fix: Lock in a consistent order (e.g., by account ID):
```java
Account first = from.getId() < to.getId() ? from : to;
Account second = (first == from) ? to : from;
synchronized (first) {
    synchronized (second) { ... }
}
```

### 9.3 Thread Pool Deadlock (Thread Starvation Deadlock)

A task submitted to a thread pool submits another task to the **same** pool and waits for
its result. If all pool threads are occupied by parent tasks waiting for child tasks that
can't run (no free threads), deadlock.

```java
ExecutorService pool = Executors.newFixedThreadPool(2);

Future<?> task1 = pool.submit(() -> {
    Future<?> child = pool.submit(() -> { ... });
    child.get();  // waits for child — but child can't run if pool is full!
});
```

Fix: Use separate pools for parent and child tasks, or use `ForkJoinPool` (work-stealing).

### 9.4 GUI Frameworks (Swing EDT)

Calling a blocking operation on the EDT (Event Dispatch Thread) while another thread waits
to run on the EDT via `SwingUtilities.invokeAndWait()`:

```
EDT: calls synchronizedMethod() — blocks waiting for a lock held by Worker Thread
Worker Thread: calls SwingUtilities.invokeAndWait() — blocks waiting for EDT
```

Fix: Never block the EDT. Use `invokeLater()` instead of `invokeAndWait()`.

---

## 10. SDE2 Interview Q&A

### Q1: What is deadlock? What are the four necessary conditions?

**A**: A deadlock occurs when two or more threads are permanently blocked, each waiting for a
resource held by another thread in the set. The four Coffman conditions are:
1. **Mutual exclusion**: Resources are held exclusively.
2. **Hold and wait**: Threads hold resources while waiting for others.
3. **No preemption**: Resources can't be forcibly taken.
4. **Circular wait**: A cycle exists in the wait-for graph.
All four must hold simultaneously. Breaking any one prevents deadlock.

### Q2: How would you detect a deadlock in production?

**A**: Multiple approaches:
1. **jstack**: `jstack <pid>` prints thread dumps and automatically detects deadlocks.
2. **ThreadMXBean**: `findDeadlockedThreads()` programmatically detects deadlocks — can be
   run by a watchdog thread on a schedule.
3. **VisualVM/JConsole**: GUI tools that invoke ThreadMXBean under the hood.
4. **JFR**: Java Flight Recorder captures lock contention events for post-mortem analysis.
5. **Thread dump analysis services**: fastThread.io, TDA.

### Q3: What is the difference between deadlock, livelock, and starvation?

**A**: **Deadlock**: Threads are blocked forever, consuming no CPU. **Livelock**: Threads are
active (consuming CPU) but make no progress — they keep reacting to each other without
completing their task. **Starvation**: A thread is runnable but never gets scheduled because
other threads monopolize the resource. Deadlock is the most severe (permanent, no CPU usage).
Livelock can resolve with random backoff. Starvation can resolve with fair locks.

### Q4: Explain the Dining Philosophers problem and how to solve it.

**A**: Five philosophers share five forks at a round table. Each needs two forks to eat. If
all grab their left fork simultaneously, all wait for their right fork — circular wait →
deadlock. Solutions:
1. **Lock ordering**: One philosopher picks up the right fork first (breaks cycle).
2. **Semaphore**: Allow at most 4 philosophers to attempt eating simultaneously.
3. **tryLock with backoff**: Try both forks; if second fails, release first and retry.
4. **Central arbitrator**: A waiter grants permission to eat.

### Q5: How does lock ordering prevent deadlock? Prove it.

**A**: Assign each lock a unique number. Require all threads to acquire locks in ascending
order. Proof by contradiction: Assume a deadlock exists. Thread T1 holds lock L_i and waits
for lock L_j (where L_j > L_i, by ordering). Thread T2 holds L_j and waits for L_k
(where L_k > L_j). Following the cycle, we'd need some thread to hold L_high and wait for
L_low, but that violates the ordering constraint. Contradiction. Therefore, no cycle can
form, and circular wait is impossible.

### Q6: What is a thread starvation deadlock? How does it differ from a classic deadlock?

**A**: A thread starvation deadlock occurs in a thread pool when all threads are blocked
waiting for results of tasks submitted to the same pool. No thread is available to execute
the pending tasks. Unlike classic deadlock (lock cycles), this involves task dependency
cycles. Fix: Use separate pools for parent and child tasks, increase pool size, or use
work-stealing pools (ForkJoinPool).

### Q7: How would you prevent deadlocks in a banking system (money transfer)?

**A**: The classic bank transfer deadlock occurs when `transfer(A→B)` locks A then B, while
`transfer(B→A)` locks B then A. Prevention: **Lock ordering** — always lock the account
with the lower ID first:
```java
Account first = (from.getId() < to.getId()) ? from : to;
Account second = (first == from) ? to : from;
synchronized (first) { synchronized (second) { /* transfer */ } }
```
This ensures all threads acquire locks in the same order, breaking circular wait.

### Q8: What does `findDeadlockedThreads()` return and what information can you extract?

**A**: It returns an array of thread IDs that are in a deadlock, or `null` if no deadlock
exists. Passing these IDs to `getThreadInfo(ids, true, true)` returns `ThreadInfo` objects
containing: thread name, state, the lock the thread is waiting on, the lock owner, the full
stack trace, and all monitors/synchronizers held. This is enough to reconstruct the
wait-for graph and pinpoint the code causing the deadlock.

### Q9: Can you have a deadlock with just one thread?

**A**: Not with intrinsic locks (`synchronized`), because they are **reentrant** — a thread
can re-acquire a lock it already holds. However, you can deadlock a single thread with a
non-reentrant lock, or by having a thread submit a task to a single-thread executor and then
call `get()` on the future (thread starvation deadlock).

### Q10: Explain how `tryLock()` with timeout prevents deadlock. What is the risk?

**A**: Instead of blocking indefinitely, `tryLock(timeout)` returns `false` if the lock
isn't acquired within the timeout. The thread then releases any locks it holds and retries.
This breaks the "hold and wait" condition. The risk is **livelock**: if multiple threads
time out simultaneously and retry at the same time, they may repeatedly fail. Mitigation:
use **random backoff** (each thread waits a random duration before retrying) to desynchronize
retry attempts.

---

## Quick Reference Cheat Sheet

```
┌──────────────────────────────────────────────────────────────────┐
│  DEADLOCK — COFFMAN'S 4 CONDITIONS                               │
│                                                                  │
│  1. Mutual Exclusion     → Use lock-free / sharable resources    │
│  2. Hold and Wait        → tryLock + release, all-or-nothing     │
│  3. No Preemption        → tryLock, timeout, interruptible       │
│  4. Circular Wait        → Lock ordering (total order)           │
│  Break ANY ONE → deadlock impossible                             │
│                                                                  │
│  DETECTION                                                       │
│  jstack <pid>                 → thread dump + deadlock detection  │
│  ThreadMXBean                 → programmatic (findDeadlockedThreads)│
│  VisualVM / JConsole          → GUI tools                        │
│                                                                  │
│  LIVELOCK vs DEADLOCK vs STARVATION                              │
│  Deadlock:    BLOCKED, 0 CPU, permanent                          │
│  Livelock:    RUNNABLE, high CPU, no progress                    │
│  Starvation:  RUNNABLE but never scheduled                       │
│                                                                  │
│  PREVENTION STRATEGIES                                           │
│  • Lock ordering (most common, most practical)                   │
│  • tryLock with timeout + random backoff                         │
│  • Avoid nested locking where possible                           │
│  • Use separate thread pools for parent/child tasks              │
│  • Prefer concurrent utilities over manual locking               │
│                                                                  │
│  DINING PHILOSOPHERS                                             │
│  5 philosophers, 5 forks. All grab left → deadlock.              │
│  Fix: asymmetric pickup, semaphore(4), tryLock, arbitrator.      │
└──────────────────────────────────────────────────────────────────┘
```
