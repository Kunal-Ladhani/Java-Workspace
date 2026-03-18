# Synchronization Deep Dive — SDE2 Interview Preparation

## Table of Contents
1. [What is Synchronization and Why We Need It](#1-what-is-synchronization-and-why-we-need-it)
2. [The Java Memory Model (JMM)](#2-the-java-memory-model-jmm)
3. [The `synchronized` Keyword](#3-the-synchronized-keyword)
4. [Monitor Lock / Intrinsic Lock](#4-monitor-lock--intrinsic-lock)
5. [Object Header and Mark Word](#5-object-header-and-mark-word)
6. [Lock Escalation](#6-lock-escalation)
7. [wait(), notify(), notifyAll()](#7-wait-notify-notifyall)
8. [Volatile vs Synchronized](#8-volatile-vs-synchronized)
9. [Synchronized and Happens-Before Guarantees](#9-synchronized-and-happens-before-guarantees)
10. [Performance Implications](#10-performance-implications)
11. [Best Practices](#11-best-practices)
12. [Common Pitfalls](#12-common-pitfalls)
13. [SDE2 Interview Q&A](#13-sde2-interview-qa)

---

## 1. What is Synchronization and Why We Need It

Synchronization is the mechanism that controls the order of access to shared mutable state by
multiple threads. Without it, concurrent threads can interleave their operations on shared data,
producing **race conditions**, **stale reads**, and **corrupted state**.

### Why We Need It

| Problem              | What happens without sync                                      |
|----------------------|----------------------------------------------------------------|
| **Atomicity**        | Compound actions (read-modify-write) get interleaved           |
| **Visibility**       | Thread A writes a value, Thread B never sees it                |
| **Ordering**         | Compiler/CPU reorders instructions, breaking logical invariants|

### The Core Contract

When a thread enters a synchronized region:
1. It acquires the **monitor lock** of the specified object.
2. All other threads attempting to acquire the same lock **block** until it is released.
3. Upon release, the changes made by the releasing thread become **visible** to the next
   thread that acquires that lock (happens-before).

---

## 2. The Java Memory Model (JMM)

The JMM (defined by JSR-133, finalized in Java 5) specifies the rules by which the JVM may
reorder memory operations, and the guarantees programs can rely on.

### 2.1 Hardware Reality

Modern CPUs have per-core caches (L1/L2) and store buffers. A write by Core-0 may sit in
its store buffer and never be flushed to main memory, so Core-1 reads a stale value. This
is not a bug — it is by design for performance.

### 2.2 Three Key Concepts

**Visibility**: When Thread A writes `x = 42`, when is Thread B guaranteed to see 42?
Without explicit synchronization, the answer is **never** — the JMM does not require it.

**Ordering (Reordering)**: The JMM allows the compiler, JIT, and CPU to reorder instructions
as long as the **single-threaded semantics** (as-if-serial) are preserved. But in a
multi-threaded context, reordering can break invariants.

**Atomicity**: A 64-bit `long` or `double` write is **not guaranteed atomic** on 32-bit JVMs
(two 32-bit writes). The JMM explicitly calls this out — use `volatile` or `synchronized` for
these types if shared.

### 2.3 Happens-Before Relationship

The happens-before (HB) relation is the foundation of the JMM. If action A happens-before
action B, then A's effects are **guaranteed visible** to B, and A is **ordered** before B.

Key HB rules:
- **Program order**: Each action in a thread HB the next action in that thread.
- **Monitor lock**: An unlock on monitor M HB every subsequent lock on M.
- **Volatile**: A write to a volatile field HB every subsequent read of that field.
- **Thread start**: `thread.start()` HB any action in the started thread.
- **Thread join**: Any action in a thread HB the return of `join()` on that thread.
- **Transitivity**: If A HB B and B HB C, then A HB C.

### 2.4 Why This Matters for `synchronized`

When Thread A exits a `synchronized` block (unlock) and Thread B enters a `synchronized`
block on the same monitor (lock), everything Thread A did **before** the unlock is visible to
Thread B **after** the lock. This is the monitor lock HB rule in action.

---

## 3. The `synchronized` Keyword

### 3.1 Synchronized Instance Method

```java
public synchronized void increment() {
    count++;
}
```

The lock object is **`this`** — the instance on which the method is invoked.
Two threads calling `increment()` on the **same** instance will synchronize.
Two threads calling it on **different** instances will NOT synchronize (different locks).

### 3.2 Synchronized Block

```java
public void increment() {
    synchronized (this) {
        count++;
    }
}
```

Functionally equivalent to the synchronized method, but gives you control over:
- **Which object** to lock on (not forced to use `this`).
- **How much code** is inside the critical section (finer granularity).

### 3.3 Synchronized Static Method

```java
public static synchronized void staticIncrement() {
    staticCount++;
}
```

The lock object is the **Class object** (`MyClass.class`). There is exactly one Class object
per classloader, so all threads synchronize regardless of which instance they hold.

### 3.4 Synchronized on Class-Level (Block Form)

```java
public void increment() {
    synchronized (MyClass.class) {
        staticCount++;
    }
}
```

Equivalent to a static synchronized method, but as a block inside an instance method.

### 3.5 Critical Distinction

| Lock on          | Protects               | Scope of exclusion            |
|------------------|------------------------|-------------------------------|
| `this`           | Instance-level state   | Per-instance                  |
| `MyClass.class`  | Static / class state   | All instances, all threads    |
| `private Object` | Whatever you decide    | Controlled, recommended       |

---

## 4. Monitor Lock / Intrinsic Lock

Every Java object has an associated **monitor** (also called intrinsic lock or mutex).
This is not a separate Java object — it is part of the object's internal structure managed
by the JVM.

### 4.1 How It Works at the Bytecode Level

When the JIT compiles a `synchronized` block, it emits:

```
monitorenter  // acquire the monitor of the object on the operand stack
  ... critical section ...
monitorexit   // release the monitor
monitorexit   // second monitorexit for the exceptional path (implicit finally)
```

- `monitorenter`: If the monitor is free, the current thread becomes the owner and the
  entry count is set to 1. If the current thread already owns it, the entry count is
  incremented (**reentrancy**). If another thread owns it, the current thread blocks.
- `monitorexit`: Decrements the entry count. When it reaches 0, the monitor is released
  and a waiting thread is woken up.

### 4.2 Reentrancy

Java intrinsic locks are **reentrant**. A thread can re-acquire a lock it already holds
without deadlocking itself:

```java
synchronized (lock) {
    synchronized (lock) {  // same thread, same lock — fine
        // ...
    }
}
```

The JVM tracks the **owning thread** and an **entry count**. Each nested `monitorenter`
increments the count; each `monitorexit` decrements it. The lock is truly released only
when the count hits 0.

### 4.3 What Happens When a Thread Blocks

When a thread cannot acquire the monitor:
1. It is placed in the **Entry Set** (also called the contention queue) of that monitor.
2. The OS thread is parked (context switched out).
3. When the owning thread releases the lock, one thread from the Entry Set is woken up
   (non-deterministic which one — no fairness guarantee).

---

## 5. Object Header and Mark Word

### 5.1 Object Layout (HotSpot JVM, 64-bit)

Every Java object in memory has:

```
+------------------+------------------+-----------------+
|    Mark Word     |  Klass Pointer   |   Fields...     |
|    (8 bytes)     |  (4/8 bytes)     |                 |
+------------------+------------------+-----------------+
```

- **Mark Word** (8 bytes on 64-bit): Contains identity hash code, GC age, lock status bits,
  and the lock record pointer or monitor pointer.
- **Klass Pointer**: Pointer to the class metadata (compressed to 4 bytes with CompressedOops).

### 5.2 Mark Word Layout (64-bit, simplified)

```
|--------------------------------------------------------------|
|  unused:25 | identity_hashcode:31 | unused:1 | age:4 | 0 01 |  Unlocked (no bias)
|--------------------------------------------------------------|
|  thread_id:54         | epoch:2    | unused:1 | age:4 | 1 01 |  Biased
|--------------------------------------------------------------|
|  lock_record_ptr:62                                   |  00  |  Lightweight locked
|--------------------------------------------------------------|
|  monitor_ptr:62                                       |  10  |  Heavyweight locked
|--------------------------------------------------------------|
|                                                       |  11  |  GC marked
|--------------------------------------------------------------|
```

The last 2-3 bits encode the **lock state**. The JVM uses these bits to implement lock
escalation without allocating additional objects (in the uncontended case).

---

## 6. Lock Escalation

The JVM optimizes locking by starting cheap and escalating only when contention arises.

### 6.1 Biased Locking (tag bits: `1 01`)

**Idea**: Most locks are acquired by only one thread. Bias the lock to that thread so that
subsequent acquisitions are essentially free (just check the thread ID in the Mark Word).

- **First acquisition**: CAS the current thread ID into the Mark Word. If successful, the
  lock is biased.
- **Subsequent acquisitions by the same thread**: Just check the thread ID — no atomic
  operation needed.
- **Revocation**: When another thread tries to acquire, biased locking is revoked at a
  **safepoint**. This is expensive, which is why biased locking was deprecated in Java 15
  (JEP 374) and disabled by default in Java 18+.

JVM flag: `-XX:+UseBiasedLocking` (on by default in Java 6-14).

### 6.2 Thin / Lightweight Lock (tag bits: `00`)

When biased locking fails (or is disabled), the JVM uses a lightweight lock:

1. Allocate a **Lock Record** in the acquiring thread's stack frame.
2. Copy the Mark Word into the Lock Record (this is called the "Displaced Mark Word").
3. **CAS** the object's Mark Word to point to the Lock Record.
4. If CAS succeeds → thread owns the thin lock.
5. If CAS fails → another thread is competing → **inflate** to a heavyweight lock.

**Unlocking**: CAS the Displaced Mark Word back into the object header. If it fails, the
lock was inflated — must release the heavyweight monitor.

### 6.3 Heavyweight Lock (tag bits: `10`)

When contention is detected (CAS fails during thin lock acquisition):

1. The JVM allocates (or reuses) an **ObjectMonitor** structure in native memory.
2. The Mark Word is updated to point to this ObjectMonitor.
3. Threads that fail to acquire use OS-level **park/unpark** (futex on Linux, WaitForSingleObject
   on Windows) → full context switch.

This is the most expensive path but is necessary when multiple threads genuinely contend.

### 6.4 Escalation Summary

```
Biased Lock ──(contention)──> Lightweight Lock ──(contention)──> Heavyweight Lock
  (free)                        (CAS spin)                         (OS mutex)
```

**Important**: Lock escalation is **one-directional** (no de-escalation back to biased
from heavyweight, though deflation of heavyweight monitors is a separate mechanism).

---

## 7. wait(), notify(), notifyAll()

These are methods on `java.lang.Object`, not on `Thread`. They form the **monitor-based
signaling** mechanism.

### 7.1 How They Work

- **`wait()`**: Releases the monitor, moves the thread to the **Wait Set**, and suspends it.
  The thread remains in the Wait Set until `notify`/`notifyAll` is called, or it is interrupted.
- **`notify()`**: Moves **one** arbitrary thread from the Wait Set to the Entry Set. That
  thread must still reacquire the monitor before proceeding.
- **`notifyAll()`**: Moves **all** threads from the Wait Set to the Entry Set. They then
  contend for the lock.

### 7.2 Why Always Inside `synchronized`?

Calling `wait()`/`notify()` outside a `synchronized` block throws `IllegalMonitorStateException`.
The reason is fundamental: these methods **release** or **signal** a monitor. If you don't own
the monitor, there is nothing to release/signal.

Moreover, without the lock protecting the condition variable, you get the **lost wakeup**
problem: the notify arrives before the wait, and the waiting thread sleeps forever.

### 7.3 The Canonical wait() Pattern

```java
synchronized (lock) {
    while (!condition) {   // WHILE, not IF
        lock.wait();
    }
    // condition is true, proceed
}
```

**Why `while` and not `if`?** Because of:
- **Spurious wakeups**: The JVM/OS may wake a thread without a corresponding `notify`.
- **Stolen signals**: Another thread that was also in the Wait Set may have been woken first,
  processed the condition, and invalidated it by the time this thread runs.

### 7.4 notify() vs notifyAll()

| Aspect             | `notify()`                        | `notifyAll()`                  |
|--------------------|-----------------------------------|--------------------------------|
| Threads woken      | Exactly one (arbitrary)           | All in Wait Set                |
| Risk               | May wake wrong thread (different conditions) | Thundering herd       |
| Use when           | All waiters wait on same condition| Multiple conditions or safety  |

**Rule of thumb**: Prefer `notifyAll()` unless you are certain only one condition is being
waited on and exactly one waiter should proceed. Using `notify()` incorrectly can lead to
threads being stuck forever (a form of livelock/starvation).

---

## 8. Volatile vs Synchronized

### 8.1 What `volatile` Provides

- **Visibility**: Writes to a volatile field are immediately flushed (via memory barriers)
  and reads always go through to main memory.
- **Ordering**: Prevents reordering of reads/writes around the volatile access (acts as a
  memory fence).
- **No atomicity for compound actions**: `volatile int count; count++;` is still a race.

### 8.2 Comparison

| Feature                   | `volatile`              | `synchronized`                |
|---------------------------|-------------------------|-------------------------------|
| Visibility                | Yes                     | Yes                           |
| Atomicity (compound)      | No                      | Yes                           |
| Ordering                  | Yes (fence)             | Yes (HB)                      |
| Blocking                  | Never blocks            | Can block on contention       |
| Use case                  | Flags, published refs   | Compound actions, invariants  |
| Can protect multiple vars | No (only the one field) | Yes (all code in the block)   |

### 8.3 When to Use Which

Use **`volatile`** for:
- Simple flags (`volatile boolean running = true;`)
- Publishing immutable objects to other threads
- Double-checked locking (the field being checked must be volatile)

Use **`synchronized`** for:
- Compound actions (`if (map.containsKey(k)) map.put(k, v)`)
- Multiple related fields that must be updated atomically
- Any read-modify-write operation

---

## 9. Synchronized and Happens-Before Guarantees

### The Guarantee

An unlock on monitor M **happens-before** every subsequent lock on monitor M.

This means: everything a thread did **before** releasing the lock is visible to any thread
that **subsequently** acquires the same lock. This includes writes to non-volatile, non-
synchronized variables — they are all flushed and made visible as a side-effect of the
monitor exit.

### Transitive Visibility

```java
// Thread A
x = 42;                  // (1)
synchronized (lock) {    // (2) lock
    y = 1;               // (3)
}                        // (4) unlock

// Thread B
synchronized (lock) {    // (5) lock  — HB from (4)
    int r1 = y;          // sees 1
    int r2 = x;          // sees 42! Even though x is not volatile/synchronized
}
```

The HB edge from (4) → (5) carries **all** of Thread A's prior writes (including `x = 42`)
into Thread B's visibility.

---

## 10. Performance Implications

### 10.1 Lock Contention

When many threads compete for the same lock:
- Threads are parked (context switch to kernel, ~5-10 μs).
- Waking a parked thread incurs another context switch.
- Cache lines containing the lock are bounced between cores (cache coherence traffic).

**Amdahl's Law** applies: the portion of your code that is serialized (inside `synchronized`)
limits your maximum speedup regardless of how many cores you add.

### 10.2 Context Switching Cost

A context switch involves saving/restoring registers, flushing TLB entries, and potentially
invalidating cache lines. On modern hardware:
- Voluntary (park/unpark): ~5-10 μs
- Involuntary (preemption): ~10-30 μs

### 10.3 Uncontended Locks Are Cheap

Thanks to biased locking and thin locks, an **uncontended** `synchronized` acquisition is
extremely cheap (often <50ns). The cost only becomes significant under contention.

### 10.4 Mitigation Strategies

- **Reduce lock scope**: Keep critical sections as short as possible.
- **Lock splitting/striping**: Use multiple locks for independent data (e.g., `ConcurrentHashMap`
  uses 16 segments, each with its own lock).
- **Read-write locks**: `ReentrantReadWriteLock` allows concurrent readers.
- **Lock-free algorithms**: Use `Atomic*` classes and CAS operations.
- **Thread-local storage**: Eliminate sharing entirely.

---

## 11. Best Practices

### 11.1 Lock on Private Final Objects

```java
// GOOD
private final Object lock = new Object();
public void doSomething() {
    synchronized (lock) { ... }
}

// BAD
public synchronized void doSomething() { ... }  // exposes 'this' as lock
```

Locking on `this` means external code can do `synchronized (yourObject)` and interfere
with your internal synchronization. A private lock prevents this.

### 11.2 Keep Critical Sections Short

Only protect the minimum code that accesses shared state. Do I/O, logging, and computation
**outside** the synchronized block.

### 11.3 Never Lock on String, Integer, or Boolean

```java
// TERRIBLE — String literals are interned (shared across the JVM)
synchronized ("LOCK") { ... }

// TERRIBLE — Integer.valueOf() caches -128 to 127
Integer lock = 42;
synchronized (lock) { ... }

// TERRIBLE — only two instances: TRUE and FALSE
Boolean lock = Boolean.TRUE;
synchronized (lock) { ... }
```

These are shared singleton objects, meaning unrelated code may lock on the same object,
causing mysterious deadlocks or contention.

### 11.4 Document Thread-Safety

Use annotations (`@ThreadSafe`, `@GuardedBy("lock")`) and clearly document which lock
protects which state.

### 11.5 Prefer Higher-Level Concurrency Utilities

Before reaching for `synchronized`, consider:
- `java.util.concurrent.locks.ReentrantLock` (tryLock, fairness, conditions)
- `java.util.concurrent.atomic.*` (AtomicInteger, AtomicReference)
- `ConcurrentHashMap`, `CopyOnWriteArrayList`
- `ExecutorService`, `CompletableFuture`

Use `synchronized` when its simplicity is sufficient and you don't need the extra features.

---

## 12. Common Pitfalls

### 12.1 Synchronizing on the Wrong Object

```java
List<String> list = Collections.synchronizedList(new ArrayList<>());

// WRONG — iterating without holding the list's lock
for (String s : list) { ... }  // ConcurrentModificationException!

// CORRECT
synchronized (list) {
    for (String s : list) { ... }
}
```

### 12.2 Double-Checked Locking (Before Java 5 / Without Volatile)

```java
// BROKEN without volatile (before Java 5 memory model fix)
if (instance == null) {
    synchronized (lock) {
        if (instance == null) {
            instance = new Singleton();  // may be reordered!
        }
    }
}
```

Without `volatile`, the write to `instance` and the constructor may be reordered — another
thread may see a non-null `instance` with uninitialized fields. Fixed by declaring
`instance` as `volatile`.

### 12.3 Locking on a Non-Final Field

```java
private Object lock = new Object();

public void reassign() {
    lock = new Object();  // now threads are locking on different objects!
}
```

Always declare lock objects as `final`.

### 12.4 Holding Locks During I/O or Long Operations

This starves other threads and kills throughput. Move I/O outside the synchronized block.

### 12.5 Nested Locking (Deadlock Risk)

Acquiring multiple locks in different orders is the classic deadlock pattern. If you must
acquire multiple locks, always do so in a consistent global order.

---

## 13. SDE2 Interview Q&A

### Q1: What is the difference between a synchronized method and a synchronized block?

**A**: A synchronized method acquires the lock on the entire `this` object (or `Class` object
for static methods) and holds it for the entire method body. A synchronized block lets you
choose which object to lock on and how much code to protect. Blocks are preferred because they
offer finer granularity and allow locking on a private object rather than exposing `this`.

### Q2: Explain the Java Memory Model in the context of synchronization.

**A**: The JMM defines happens-before relationships that determine when writes by one thread
become visible to another. Without synchronization, the JMM allows the compiler, JIT, and CPU
to reorder instructions and cache values, meaning Thread B may never see Thread A's writes.
`synchronized` establishes a happens-before edge: everything done before an unlock is visible
to everything after the next lock on the same monitor. This provides both visibility and
ordering guarantees.

### Q3: What is lock escalation in the JVM? Explain the stages.

**A**: HotSpot JVM implements three lock levels:
1. **Biased lock**: The lock is "biased" to the first thread that acquires it. Subsequent
   acquisitions by the same thread require only a thread-ID check (no CAS). Revoked at a
   safepoint if another thread competes.
2. **Thin/Lightweight lock**: Uses CAS to swap a pointer to a Lock Record in the thread's
   stack into the object's Mark Word. No OS-level blocking.
3. **Heavyweight lock**: Allocates an ObjectMonitor. Blocking threads are parked via OS
   primitives (futex/mutex). Full context switch.

Escalation goes biased → thin → heavy. It does not de-escalate.

### Q4: Why must wait() be called inside a synchronized block? What is a spurious wakeup?

**A**: `wait()` releases the monitor — you must own it first. Without the surrounding
`synchronized`, you'd get `IllegalMonitorStateException`. More importantly, the lock protects
the condition being checked. Without it, a `notify()` could fire between checking the condition
and calling `wait()`, causing a lost wakeup.

A spurious wakeup is when `wait()` returns even though no thread called `notify()`/`notifyAll()`.
This is allowed by the POSIX threading specification and JVM implementations for efficiency.
The fix is to always call `wait()` in a `while` loop that re-checks the condition.

### Q5: Can two threads enter two different synchronized methods on the same object simultaneously?

**A**: No. Both synchronized instance methods lock on `this`. Since there is only one intrinsic
lock per object, only one thread can hold it at a time, so they are mutually exclusive.

However, a synchronized *instance* method and a synchronized *static* method on the same
class CAN run concurrently, because they lock on different objects (`this` vs `Class`).

### Q6: What is the difference between volatile and synchronized?

**A**: `volatile` provides visibility and ordering but NOT atomicity for compound actions.
`synchronized` provides all three. `volatile` never blocks; `synchronized` can.

Use `volatile` for simple flags and single-variable publication. Use `synchronized` (or
`Atomic*` classes) for compound actions like check-then-act or read-modify-write.

### Q7: Explain the Mark Word and how locks are stored in the object header.

**A**: Every Java object has an 8-byte Mark Word in its header. The last 2-3 bits encode the
lock state: `01` for unlocked/biased, `00` for lightweight-locked, `10` for heavyweight-locked,
`11` for GC-marked. In the biased state, the Mark Word stores the biased thread's ID. In
lightweight state, it stores a pointer to the Lock Record on the stack. In heavyweight state,
it stores a pointer to the ObjectMonitor.

### Q8: What happens internally when a thread cannot acquire a monitor?

**A**: The thread is placed in the monitor's Entry Set (contention queue). The JVM parks the
thread using OS primitives (`futex_wait` on Linux). This involves a user-to-kernel context
switch. When the owning thread releases the lock, the JVM unparks one (or more) threads from
the Entry Set. The woken thread must still CAS to acquire the monitor; if it fails (e.g.,
another thread grabbed it first), it goes back to the Entry Set.

### Q9: Why should you never lock on a String literal or a cached Integer?

**A**: String literals are interned — all occurrences of `"LOCK"` in the JVM point to the
same object. Similarly, `Integer.valueOf(42)` returns a cached instance for values -128 to
127. Locking on these means completely unrelated code (possibly in different libraries) may
inadvertently synchronize on the same object, causing deadlocks or severe contention.

### Q10: How does synchronized relate to Amdahl's Law?

**A**: Amdahl's Law states that the maximum speedup of a parallelized program is limited by
its serial fraction. Synchronized blocks are serial — only one thread executes at a time.
If 10% of your code is in synchronized blocks, the theoretical max speedup is 10x regardless
of core count. This is why minimizing the scope and frequency of synchronization is critical
for scalability.

---

## Quick Reference Cheat Sheet

```
┌───────────────────────────────────────────────────────────────────┐
│  SYNCHRONIZED KEYWORD                                             │
│                                                                   │
│  Instance method    → locks on 'this'                             │
│  Static method      → locks on 'ClassName.class'                  │
│  Block              → locks on specified object                   │
│                                                                   │
│  LOCK ESCALATION (HotSpot)                                        │
│  Biased → Thin (CAS) → Heavy (OS mutex)                          │
│                                                                   │
│  HAPPENS-BEFORE                                                   │
│  unlock(M) ──HB──> lock(M)                                       │
│  All writes before unlock are visible after lock                  │
│                                                                   │
│  WAIT/NOTIFY                                                      │
│  Always in while loop inside synchronized                         │
│  wait() → releases lock, enters Wait Set                          │
│  notify() → moves one thread: Wait Set → Entry Set                │
│  notifyAll() → moves all threads: Wait Set → Entry Set            │
│                                                                   │
│  BEST PRACTICES                                                   │
│  • Lock on private final Object                                   │
│  • Minimize critical section                                      │
│  • Never lock on String/Integer/Boolean                           │
│  • Document @GuardedBy                                            │
│  • Prefer j.u.c utilities when features needed                    │
└───────────────────────────────────────────────────────────────────┘
```
