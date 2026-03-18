# Java Concurrency — Complete Learning Roadmap (SDE2)

> Follow this roadmap **top to bottom**. Each section builds on the previous one.
> Every topic has detailed notes (400-800+ lines) and runnable Java demos.

---

## Phase 1: Thread Fundamentals

_Understand what threads are, how to create them, and how they behave._

| # | Topic | Notes | Key Demos |
|---|-------|-------|-----------|
| 1 | **Thread Creation** — Runnable, Callable, Thread class, FutureTask, ExecutorService, Lambdas | [LEARNING_NOTES.md](thread_creation/LEARNING_NOTES.md) | [ThreadCreationDemo.java](thread_creation/ThreadCreationDemo.java), [CallableVsRunnableDemo.java](thread_creation/CallableVsRunnableDemo.java) |
| 2 | **Thread Lifecycle** — All 6 states (NEW → RUNNABLE → BLOCKED/WAITING/TIMED_WAITING → TERMINATED), daemon threads, interruption, priorities | [LEARNING_NOTES.md](thread_lifecycle_deep_dive/LEARNING_NOTES.md) | [ThreadLifecycleDemo.java](thread_lifecycle_deep_dive/ThreadLifecycleDemo.java) |
| 3 | **wait / join / notify / notifyAll / sleep** — Inter-thread communication, the comparison table you'll be asked in every interview | [LEARNING_NOTES.md](wait_notify_join_sleep/LEARNING_NOTES.md) | [WaitNotifyDemo.java](wait_notify_join_sleep/WaitNotifyDemo.java), [SleepVsWaitDemo.java](wait_notify_join_sleep/SleepVsWaitDemo.java) |

---

## Phase 2: The Problems — Why Concurrency is Hard

_Before learning solutions, understand the problems._

| # | Topic | Notes | Key Demos |
|---|-------|-------|-----------|
| 4 | **Race Condition** — check-then-act, read-modify-write, why `counter++` is not atomic, data race vs race condition | [LEARNING_NOTES.md](race_condition/LEARNING_NOTES.md) | [RaceConditionDemo.java](race_condition/RaceConditionDemo.java) |
| 5 | **Deadlock** — Coffman's 4 conditions, detection (jstack, ThreadMXBean), prevention strategies, livelock vs starvation | [LEARNING_NOTES.md](deadlock_deep_dive/LEARNING_NOTES.md) | [DeadlockDemo.java](deadlock_deep_dive/DeadlockDemo.java) |

---

## Phase 3: Basic Locking — synchronized & Mutex

_The first solutions: intrinsic locks and mutual exclusion._

| # | Topic | Notes | Key Demos |
|---|-------|-------|-----------|
| 6 | **Synchronization** — `synchronized` keyword, monitor locks, JMM, happens-before, lock escalation (biased → thin → heavyweight), volatile | [LEARNING_NOTES.md](synchronization_deep_dive/LEARNING_NOTES.md) | [SynchronizationDemo.java](synchronization_deep_dive/SynchronizationDemo.java) |
| 7 | **Mutex** — Concept of mutual exclusion, mutex vs binary semaphore (ownership!), mutex vs monitor vs lock terminology | [LEARNING_NOTES.md](mutex/LEARNING_NOTES.md) | [MutexDemo.java](mutex/MutexDemo.java) |

---

## Phase 4: Explicit Locks — ReentrantLock & Friends

_Beyond `synchronized`: flexible, powerful, explicit locks._

| # | Topic | Notes | Key Demos |
|---|-------|-------|-----------|
| 8 | **ReentrantLock** — reentrancy, AQS internals, lock/tryLock/lockInterruptibly, Condition variables (await/signal), deadlock prevention with tryLock | [LEARNING_NOTES.md](reenterant_lock/LEARNING_NOTES.md) | [ReentrantLockDemo.java](reenterant_lock/ReentrantLockDemo.java), [ConditionVariableDemo.java](reenterant_lock/ConditionVariableDemo.java), [DeadlockPreventionDemo.java](reenterant_lock/DeadlockPreventionDemo.java) |
| 9 | **Fair Lock** — Fair vs non-fair, barging, throughput tradeoff, tryLock() fairness caveat | [FAIR_LOCK_NOTES.md](reenterant_lock/FAIR_LOCK_NOTES.md) | [FairLockDemo.java](reenterant_lock/FairLockDemo.java) |
| 10 | **FIFO Lock** — FIFO = Fair ordering, CLH queue, Ticket Lock, MCS Lock | [FIFO_LOCK_NOTES.md](reenterant_lock/FIFO_LOCK_NOTES.md) | [FIFOLockDemo.java](reenterant_lock/FIFOLockDemo.java) |

---

## Phase 5: Advanced Locks

_Specialized locks for read-heavy workloads and optimistic strategies._

| # | Topic | Notes | Key Demos |
|---|-------|-------|-----------|
| 11 | **ReadWriteLock** — Shared lock (read) vs Exclusive lock (write), lock downgrading, writer starvation, AQS state splitting | [LEARNING_NOTES.md](read_write_lock/LEARNING_NOTES.md) | [ReadWriteLockDemo.java](read_write_lock/ReadWriteLockDemo.java) |
| 12 | **StampedLock** — 3 modes (write, read, optimistic read), stamps, lock conversion, when to use over ReadWriteLock | [LEARNING_NOTES.md](stamped_lock/LEARNING_NOTES.md) | [StampedLockDemo.java](stamped_lock/StampedLockDemo.java) |
| 13 | **Spin Lock** — Busy-waiting, TAS, TTAS, CLH Lock, MCS Lock, adaptive spinning in JVM | [LEARNING_NOTES.md](spin_lock/LEARNING_NOTES.md) | [SpinLockDemo.java](spin_lock/SpinLockDemo.java) |

---

## Phase 6: Locking Strategies

_Not specific classes — these are design strategies that appear everywhere._

| # | Topic | Notes | Key Demos |
|---|-------|-------|-----------|
| 14 | **Pessimistic Locking** — Lock first, ask questions later. synchronized, ReentrantLock, SELECT FOR UPDATE | [LEARNING_NOTES.md](pessimistic_lock/LEARNING_NOTES.md) | [PessimisticLockDemo.java](pessimistic_lock/PessimisticLockDemo.java) |
| 15 | **Optimistic Locking** — Assume no conflict, detect after. CAS, AtomicInteger, ABA problem, version columns | [LEARNING_NOTES.md](optimistic_lock/LEARNING_NOTES.md) | [OptimisticLockDemo.java](optimistic_lock/OptimisticLockDemo.java) |

---

## Phase 7: Concurrency Primitives

_Higher-level building blocks from `java.util.concurrent`._

| # | Topic | Notes | Key Demos |
|---|-------|-------|-----------|
| 16 | **Semaphore** — Counting semaphore, Dijkstra's P/V, permits, rate limiting, connection pooling | [LEARNING_NOTES.md](semaphore/LEARNING_NOTES.md) | [SemaphoreDemo.java](semaphore/SemaphoreDemo.java) |

---

## How to Study This

### For Learning (follow the order above):
```
Phase 1 → 2 → 3 → 4 → 5 → 6 → 7
```

### For Quick Interview Revision:
Focus on these high-frequency topics:
1. Thread Lifecycle (states + transitions)
2. synchronized vs ReentrantLock (comparison table)
3. wait vs sleep (the #1 most asked question)
4. Race Condition (why counter++ isn't atomic)
5. Deadlock (Coffman's conditions + prevention)
6. ReentrantLock (tryLock, Condition, AQS internals)
7. Pessimistic vs Optimistic locking
8. ReadWriteLock (shared vs exclusive)

### For Each Topic:
1. Read the `LEARNING_NOTES.md` thoroughly
2. Run the `*Demo.java` files and observe the output
3. Review the Interview Q&As at the bottom of each notes file
4. Try modifying the demos to experiment

---

## Directory Structure

```
concurrency/
├── CONCURRENCY_ROADMAP.md          ← YOU ARE HERE
│
├── thread_creation/                 ← Phase 1
│   ├── LEARNING_NOTES.md
│   ├── ThreadCreationDemo.java
│   └── CallableVsRunnableDemo.java
│
├── thread_lifecycle_deep_dive/      ← Phase 1
│   ├── LEARNING_NOTES.md
│   └── ThreadLifecycleDemo.java
│
├── wait_notify_join_sleep/          ← Phase 1
│   ├── LEARNING_NOTES.md
│   ├── WaitNotifyDemo.java
│   └── SleepVsWaitDemo.java
│
├── race_condition/                  ← Phase 2
│   ├── LEARNING_NOTES.md
│   └── RaceConditionDemo.java
│
├── deadlock_deep_dive/              ← Phase 2
│   ├── LEARNING_NOTES.md
│   └── DeadlockDemo.java
│
├── synchronization_deep_dive/       ← Phase 3
│   ├── LEARNING_NOTES.md
│   └── SynchronizationDemo.java
│
├── mutex/                           ← Phase 3
│   ├── LEARNING_NOTES.md
│   └── MutexDemo.java
│
├── reenterant_lock/                 ← Phase 4
│   ├── LEARNING_NOTES.md
│   ├── FAIR_LOCK_NOTES.md
│   ├── FIFO_LOCK_NOTES.md
│   ├── ReentrantLockDemo.java
│   ├── ConditionVariableDemo.java
│   ├── DeadlockPreventionDemo.java
│   ├── FairLockDemo.java
│   ├── FIFOLockDemo.java
│   ├── SharedResource.java          (your original code)
│   └── Driver.java                  (your original code)
│
├── read_write_lock/                 ← Phase 5
│   ├── LEARNING_NOTES.md
│   └── ReadWriteLockDemo.java
│
├── stamped_lock/                    ← Phase 5
│   ├── LEARNING_NOTES.md
│   └── StampedLockDemo.java
│
├── spin_lock/                       ← Phase 5
│   ├── LEARNING_NOTES.md
│   └── SpinLockDemo.java
│
├── pessimistic_lock/                ← Phase 6
│   ├── LEARNING_NOTES.md
│   └── PessimisticLockDemo.java
│
├── optimistic_lock/                 ← Phase 6
│   ├── LEARNING_NOTES.md
│   └── OptimisticLockDemo.java
│
├── semaphore/                       ← Phase 7
│   ├── LEARNING_NOTES.md
│   └── SemaphoreDemo.java
│
└── (your existing packages: synchronization/, deadlock_problem/,
     join_method/, producer_consumer_problem/, etc.)
```

---

> **Total: 16 topics, 16 notes files (~10,000+ lines of notes), 18 runnable demos.**
> Good luck with the SDE2 prep!
