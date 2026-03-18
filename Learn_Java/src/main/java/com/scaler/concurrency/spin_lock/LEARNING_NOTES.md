# Spin Lock — Complete Guide for SDE2

## Table of Contents

1. [What is a Spin Lock?](#1-what-is-a-spin-lock)
2. [How Spin Locks Work](#2-how-spin-locks-work)
3. [Spin Lock vs Blocking Lock](#3-spin-lock-vs-blocking-lock)
4. [Implementing a Spin Lock with AtomicBoolean and CAS](#4-implementing-a-spin-lock-with-atomicboolean-and-cas)
5. [Test-And-Set (TAS) Spin Lock](#5-test-and-set-tas-spin-lock)
6. [Test-And-Test-And-Set (TTAS) Spin Lock](#6-test-and-test-and-set-ttas-spin-lock)
7. [CLH Lock — Queue-Based Spin Lock](#7-clh-lock--queue-based-spin-lock)
8. [MCS Lock — Another Queue-Based Spin Lock](#8-mcs-lock--another-queue-based-spin-lock)
9. [Adaptive Spinning](#9-adaptive-spinning)
10. [CPU Cache Effects and Cache Line Bouncing](#10-cpu-cache-effects-and-cache-line-bouncing)
11. [Spin Lock and CPU Usage](#11-spin-lock-and-cpu-usage)
12. [When to Use Spin Locks in Practice](#12-when-to-use-spin-locks-in-practice)
13. [Interview Questions & Answers](#13-interview-questions--answers)

---

## 1. What is a Spin Lock?

A **spin lock** is a synchronization primitive where a thread waiting to acquire the lock
**continuously polls** (spins) in a loop, checking whether the lock has become available,
instead of being put to sleep.

```
Blocking Lock (e.g., ReentrantLock):
  Thread → tries to acquire → lock is held → thread is PARKED (sleeps)
  Later: lock released → thread is UNPARKED (woken up) → acquires lock

Spin Lock:
  Thread → tries to acquire → lock is held → while(true) { try again... }
  Thread burns CPU cycles spinning until the lock becomes free
```

The key insight: **spinning avoids the overhead of context switching** (parking/unparking
a thread involves OS kernel calls, saving/restoring registers, cache flushing). But
spinning **wastes CPU** — the thread does nothing useful while spinning.

### When Spinning Wins

If the lock is held for a **very short time** (nanoseconds to low microseconds), the cost
of context switching exceeds the cost of spinning. In this case, spinning is more efficient.

### When Spinning Loses

If the lock is held for a **long time** (milliseconds or more), the spinning thread wastes
CPU for the entire duration. Blocking is far better here — the sleeping thread consumes
zero CPU.

---

## 2. How Spin Locks Work

### Basic Mechanism

```java
// Conceptual spin lock
AtomicBoolean locked = new AtomicBoolean(false);

// Lock (acquire)
while (!locked.compareAndSet(false, true)) {
    // spin — keep trying
}

// Unlock (release)
locked.set(false);
```

The lock is a single boolean (or integer) flag. To acquire, a thread atomically attempts
to change the flag from `false` (unlocked) to `true` (locked) using **Compare-And-Swap (CAS)**.
If CAS fails (someone else holds the lock), the thread retries in a loop.

### CAS (Compare-And-Swap)

CAS is a **hardware-level atomic instruction** (e.g., `CMPXCHG` on x86) that:

```
CAS(memory_location, expected_value, new_value):
  if memory_location == expected_value:
      memory_location = new_value
      return true   // success
  else:
      return false  // another thread modified it
```

CAS is **lock-free** — it doesn't require kernel involvement. This is why spin locks can
be extremely fast for short critical sections.

### The Spin Loop

```
Thread A: CAS(false→true) → SUCCESS → enters critical section
Thread B: CAS(false→true) → FAIL (value is true, not false)
          CAS(false→true) → FAIL
          CAS(false→true) → FAIL  (spinning...)
          ...
Thread A: set(false)       → releases lock
Thread B: CAS(false→true) → SUCCESS → enters critical section
```

---

## 3. Spin Lock vs Blocking Lock

| Aspect                     | Spin Lock                      | Blocking Lock (e.g., ReentrantLock)   |
|----------------------------|--------------------------------|---------------------------------------|
| **Wait mechanism**         | Busy-waiting (loop)            | Thread parks (sleeps)                 |
| **CPU during wait**        | 100% (burns CPU)               | ~0% (sleeping)                        |
| **Context switch**         | None                           | Yes (park + unpark = 2 switches)      |
| **Best for**               | Very short critical sections   | Long critical sections                |
| **Latency to acquire**     | Very low (no wake-up delay)    | Higher (kernel must schedule thread)  |
| **Fairness**               | Typically unfair               | Can be fair (FIFO)                    |
| **Starvation**             | Possible                       | Prevented with fair mode              |
| **CPU core requirement**   | Needs multiple cores            | Works on single core too              |
| **Common use**             | OS kernels, JVM internals      | Application code                      |

### The Break-Even Point

The decision between spinning and blocking depends on the **expected wait time** vs the
**context switch cost**:

```
Cost of spinning  = spin_duration × CPU_cost_per_cycle
Cost of blocking  = context_switch_in + context_switch_out + scheduling_delay

If expected_wait < context_switch_cost → SPIN
If expected_wait > context_switch_cost → BLOCK
```

On modern hardware, a context switch costs roughly **1-10 microseconds**. If the critical
section is shorter than that, spinning wins. If it's longer, blocking wins.

### Single-Core Problem

On a **single CPU core**, spin locks are always worse than blocking locks. The spinning
thread holds the core and the lock-holding thread can't run to release the lock. The
spinner must be preempted by the OS scheduler first — defeating the purpose of spinning.

---

## 4. Implementing a Spin Lock with AtomicBoolean and CAS

### Basic TAS (Test-And-Set) Spin Lock

```java
import java.util.concurrent.atomic.AtomicBoolean;

public class SimpleSpinLock {
    private final AtomicBoolean locked = new AtomicBoolean(false);

    public void lock() {
        while (!locked.compareAndSet(false, true)) {
            // spin
        }
    }

    public void unlock() {
        locked.set(false);
    }
}
```

This is the simplest possible spin lock. `compareAndSet(false, true)` atomically checks
if the lock is free (`false`) and acquires it (`true`). If another thread holds it, CAS
returns false and the loop continues.

### Why AtomicBoolean?

`AtomicBoolean` uses `sun.misc.Unsafe` (or `VarHandle` in newer Java) to perform CAS
operations that map directly to hardware atomic instructions. This guarantees:

1. **Atomicity**: the check-and-set is a single indivisible operation.
2. **Visibility**: changes are immediately visible to all threads (volatile semantics).
3. **No locks needed**: CAS is lock-free at the hardware level.

---

## 5. Test-And-Set (TAS) Spin Lock

The TAS lock is the basic spin lock from Section 4. On every iteration, it executes a CAS
instruction.

```java
public void lock() {
    while (!locked.compareAndSet(false, true)) {
        // every iteration does a CAS (write operation)
    }
}
```

### Problem: Cache Line Bouncing

Every CAS instruction is a **write operation** to shared memory. On multi-core CPUs, this
causes the cache line containing the lock to be **invalidated** on all other cores every
time any core attempts CAS. This is called **cache line bouncing** or a **cache coherence
storm**.

```
Core 0: CAS → write → invalidate cache on Core 1, 2, 3
Core 1: CAS → write → invalidate cache on Core 0, 2, 3
Core 2: CAS → write → invalidate cache on Core 0, 1, 3
Core 3: CAS → write → invalidate cache on Core 0, 1, 2

Each CAS causes N-1 cache invalidations. With N cores spinning,
that's O(N²) bus traffic per "round" of spinning.
```

This is extremely bad for performance under contention. The memory bus becomes saturated
with invalidation messages, slowing down ALL cores — even the one in the critical section.

---

## 6. Test-And-Test-And-Set (TTAS) Spin Lock

The TTAS optimization: **read first, CAS only if the lock looks free**.

```java
public class TTASSpinLock {
    private final AtomicBoolean locked = new AtomicBoolean(false);

    public void lock() {
        while (true) {
            // TEST: read the lock value (cheap — read from local cache)
            while (locked.get()) {
                // spin on local cache — no bus traffic!
            }

            // TEST-AND-SET: lock looks free, attempt CAS
            if (locked.compareAndSet(false, true)) {
                return; // acquired!
            }
            // CAS failed — someone else got it first, go back to spinning on read
        }
    }

    public void unlock() {
        locked.set(false);
    }
}
```

### Why TTAS is Better

1. **Inner loop reads only**: `locked.get()` reads from the core's local cache. As long as
   the lock is held, the cached value stays `true` and no bus traffic is generated.

2. **CAS only when hopeful**: CAS is attempted only when the read shows `false` (lock
   might be free). This dramatically reduces the number of write operations on the bus.

3. **Cache coherence**: When the lock holder releases (`locked.set(false)`), the MESI
   protocol invalidates the cache line on all spinning cores. They re-read, see `false`,
   and ONE of them succeeds at CAS.

### Performance Comparison

```
Under high contention:
  TAS:  O(N²) bus transactions per lock transfer (every spin iteration does CAS)
  TTAS: O(N) bus transactions per lock transfer (invalidation + re-read on release)
```

TTAS is a **massive improvement** over TAS, but still has the "thundering herd" problem:
when the lock is released, ALL spinning threads attempt CAS simultaneously, and all but
one fail.

### Further Optimization: Exponential Backoff

```java
public void lock() {
    int delay = MIN_DELAY;
    while (true) {
        while (locked.get()) { /* spin on read */ }

        if (locked.compareAndSet(false, true)) {
            return;
        }

        // CAS failed — back off to reduce contention
        Thread.onSpinWait(); // or LockSupport.parkNanos(delay)
        delay = Math.min(delay * 2, MAX_DELAY);
    }
}
```

Backoff reduces the thundering herd effect by spreading out retry attempts over time.

---

## 7. CLH Lock — Queue-Based Spin Lock

**CLH Lock** (Craig, Landin, Hagersten, 1993) is a queue-based spin lock where each thread
spins on its **predecessor's node** instead of on a shared variable.

### How It Works

```
Each thread creates a node. The node has a boolean "locked" field.
Threads form an implicit linked list (queue).

Thread spins on predecessor's node:
  - If predecessor's locked == true → keep spinning
  - If predecessor's locked == false → predecessor released, it's my turn

    ┌────────┐    ┌────────┐    ┌────────┐
    │ Node A │←── │ Node B │←── │ Node C │    (tail →)
    │ locked │    │ locked │    │ locked │
    │ =false │    │ =true  │    │ =true  │
    └────────┘    └────────┘    └────────┘
    (released)    (spins on A)  (spins on B)
```

### Implementation Sketch

```java
public class CLHLock {
    private final AtomicReference<Node> tail = new AtomicReference<>(new Node());
    private final ThreadLocal<Node> myNode = ThreadLocal.withInitial(Node::new);
    private final ThreadLocal<Node> myPred = new ThreadLocal<>();

    public void lock() {
        Node node = myNode.get();
        node.locked = true;

        // Atomically append to tail, get predecessor
        Node pred = tail.getAndSet(node);
        myPred.set(pred);

        // Spin on predecessor's locked field
        while (pred.locked) {
            Thread.onSpinWait();
        }
    }

    public void unlock() {
        Node node = myNode.get();
        node.locked = false;        // signal successor

        myNode.set(myPred.get());   // reuse predecessor's node
    }

    private static class Node {
        volatile boolean locked = false;
    }
}
```

### Advantages of CLH

1. **No thundering herd**: Each thread spins on a DIFFERENT variable (its predecessor's node).
   When the lock is released, only ONE thread (the direct successor) notices.

2. **FIFO fairness**: Threads acquire in the order they joined the queue. No starvation.

3. **O(1) bus traffic per release**: Only the successor's cache line is invalidated.

### Disadvantage

- On **NUMA architectures** (Non-Uniform Memory Access), a thread might be spinning on a
  cache line that's in a remote memory node — slow access. MCS Lock solves this.

---

## 8. MCS Lock — Another Queue-Based Spin Lock

**MCS Lock** (Mellor-Crummey, Scott, 1991) is similar to CLH but each thread spins on
**its own node** instead of the predecessor's node.

### How It Works

```
Each thread has its own node with a "locked" boolean.
When thread wants the lock, it appends its node to the queue.
The predecessor explicitly sets successor's locked = false when releasing.

    ┌────────┐    ┌────────┐    ┌────────┐
    │ Node A │───→│ Node B │───→│ Node C │    (tail →)
    │ locked │    │ locked │    │ locked │
    │ =false │    │ =true  │    │ =true  │
    └────────┘    └────────┘    └────────┘
    (holds lock)  (spins on     (spins on
                   OWN node)     OWN node)

    When A releases: A sets B.locked = false → B enters critical section
```

### Key Difference from CLH

| Aspect                    | CLH Lock                     | MCS Lock                      |
|---------------------------|------------------------------|-------------------------------|
| Spins on                  | Predecessor's node           | Own node                      |
| NUMA-friendly             | No (remote spinning)         | Yes (local spinning)          |
| Memory per thread         | 1 node                       | 1 node                        |
| Release mechanism         | Set own node's flag          | Set successor's node flag     |
| Implementation complexity | Simpler                      | Slightly more complex         |

### NUMA Advantage

In NUMA systems, accessing local memory is much faster than remote memory. Since MCS threads
spin on their OWN node (which is in their local memory), they avoid the penalty of spinning
on a remote node. This makes MCS Lock the preferred choice for NUMA-aware systems.

### Where They're Used

- **Linux kernel**: uses MCS-based `qspinlock` since kernel 3.15 (2014).
- **Java's AQS**: uses a CLH-variant queue for its wait queue.
- **Database engines**: some use MCS/CLH for internal latch management.

---

## 9. Adaptive Spinning

**Adaptive spinning** is a hybrid approach: spin for a while, then block if the lock is
still not available. This combines the low-latency of spinning with the CPU-efficiency
of blocking.

### How It Works

```
Thread tries to acquire lock:
  1. SPIN for a short duration (typically based on past success)
  2. If lock acquired during spin → great, no context switch
  3. If spin budget exhausted → BLOCK (park the thread)
  4. When lock is released → unpark the blocked thread
```

### Java's synchronized Uses Adaptive Spinning

The HotSpot JVM implements adaptive spinning for `synchronized` blocks. The spin count
adapts based on the **success history** of spinning on the same lock:

```
Initial attempt:
  - Spin for N iterations (JVM-determined, typically ~100)
  
If spinning recently succeeded on this lock:
  - Increase spin count (spinning pays off here)
  
If spinning recently failed on this lock:
  - Decrease spin count (or skip spinning entirely)
  
Eventually, if spin fails:
  - Inflate the lock to a heavyweight monitor
  - Park the thread (OS-level blocking)
```

### Lock Inflation Levels in HotSpot JVM

```
Biased Lock        → Thin Lock        → Heavyweight Monitor
(zero overhead)      (CAS + short spin)  (OS mutex + park/unpark)

No contention        Brief contention    Sustained contention
Single thread        Spin succeeds       Spin fails → block
```

1. **Biased locking** (removed in JDK 15+): if only one thread uses a lock, essentially
   zero overhead.
2. **Thin lock**: CAS-based, with short adaptive spinning.
3. **Heavyweight monitor**: full OS mutex with thread parking.

### JVM Flags (for Tuning/Learning — Don't Use in Production)

```
-XX:+UseSpinning           (enable spinning — on by default)
-XX:PreBlockSpin=10        (initial spin count before blocking)
```

---

## 10. CPU Cache Effects and Cache Line Bouncing

Understanding cache effects is critical for understanding why naive spin locks perform
poorly under contention.

### Cache Coherence Protocols (MESI)

Modern CPUs use the **MESI protocol** (Modified, Exclusive, Shared, Invalid) to keep
caches consistent:

```
State       Meaning
─────       ───────
Modified    This core has the only copy, it's been modified
Exclusive   This core has the only copy, it's clean
Shared      Multiple cores have this cache line, all clean
Invalid     This cache line is stale / not present
```

### What Happens with TAS Spin Lock

```
Scenario: 4 cores spinning on the same lock variable

1. Core 0 does CAS (write) → cache line moves to Modified on Core 0
   → cache line Invalidated on Cores 1, 2, 3

2. Core 1 does CAS (write) → cache line moves to Modified on Core 1
   → cache line Invalidated on Cores 0, 2, 3

3. Core 2 does CAS (write) → ...

Each CAS triggers a cache line transfer over the bus.
With N cores: O(N) invalidations per CAS attempt.
With N cores all spinning: O(N²) bus transactions per "round."
```

This is called **cache line bouncing** or **bus storm**. It saturates the memory bus and
degrades performance for ALL cores — including the one doing useful work in the critical
section.

### Why TTAS Helps

With TTAS, spinning threads read from local cache (Shared state). No bus traffic occurs
during read-only spinning. Bus traffic only happens:

1. When the lock holder writes `false` (invalidation → Shared).
2. When competing threads attempt CAS (Shared → Modified).

This reduces bus traffic from O(N²) per round to O(N) per lock release.

### False Sharing

If the lock variable shares a cache line with other data, unrelated writes to adjacent
data can invalidate the cache line and disrupt spinning threads. Use `@Contended`
annotation (JDK 8+) or manual padding to prevent false sharing:

```java
// Manual padding to avoid false sharing
class PaddedSpinLock {
    volatile long p1, p2, p3, p4, p5, p6, p7; // padding (7 × 8 = 56 bytes)
    volatile boolean locked;
    volatile long p8, p9, p10, p11, p12, p13, p14; // padding
}
```

---

## 11. Spin Lock and CPU Usage

### Spinning Burns CPU

A spinning thread executes instructions continuously (tight loop). The CPU core is 100%
utilized doing nothing useful. This has consequences:

1. **Power consumption**: spinning prevents the core from entering low-power states.
2. **Thermal throttling**: sustained spinning heats up the CPU, potentially reducing clock speed.
3. **Resource starvation**: on a shared system, spinning steals CPU from other threads/processes.
4. **Hyper-threading interference**: on hyper-threaded cores, a spinning logical core steals
   execution resources from its sibling.

### Thread.onSpinWait() — Java 9+

Java 9 introduced `Thread.onSpinWait()` as a **spin loop hint** to the CPU:

```java
while (locked.get()) {
    Thread.onSpinWait(); // hint: "I'm spinning, optimize power"
}
```

On x86, this maps to the `PAUSE` instruction, which:
- Reduces power consumption during spinning.
- Prevents the CPU pipeline from speculatively executing the loop too aggressively.
- On hyper-threaded cores, yields execution resources to the sibling thread.
- Has near-zero overhead when not spinning.

**Always use `Thread.onSpinWait()` in spin loops in Java.**

### Hybrid Approach: Spin Then Block

In practice, the best strategy is often to spin briefly, then block:

```java
public void lock() {
    for (int i = 0; i < SPIN_LIMIT; i++) {
        if (tryAcquire()) return;
        Thread.onSpinWait();
    }
    // spin budget exhausted — block
    blockingAcquire();
}
```

This is exactly what Java's `synchronized` and `ReentrantLock` do internally.

---

## 12. When to Use Spin Locks in Practice

### In Application Code: Almost Never

For typical Java application code, **don't use spin locks**. Use `synchronized` or
`ReentrantLock` instead. They already incorporate adaptive spinning internally and handle
the spin-to-block transition automatically.

### Where Spin Locks Are Actually Used

1. **OS Kernels**: Linux's `spinlock_t` is used for very short critical sections in
   interrupt handlers (where blocking is not possible).

2. **JVM Internals**: HotSpot uses spinning during lock inflation (biased → thin →
   heavyweight) and in the garbage collector.

3. **Database Engines**: Internal latches for buffer pool pages, lock manager structures.

4. **High-Frequency Trading**: Latency-critical paths where microseconds matter and context
   switch overhead is unacceptable.

5. **Lock-Free Data Structures**: CAS retry loops in `ConcurrentHashMap`, `AtomicInteger`,
   etc. are essentially short spin loops.

### Guidelines

| Scenario                                 | Recommendation            |
|------------------------------------------|---------------------------|
| Critical section < 1 microsecond         | Spin lock MAY help        |
| Critical section > 1 microsecond         | Use blocking lock         |
| Single-core CPU                          | NEVER use spin lock       |
| Application code                         | Use synchronized/ReentrantLock |
| OS kernel / interrupt context            | Spin lock is appropriate  |
| Need fairness                            | Use CLH/MCS or blocking lock |
| Maximum throughput, low contention       | Spin lock MAY help        |
| Maximum throughput, high contention      | Use blocking lock         |

---

## 13. Interview Questions & Answers

### Q1: What is a spin lock? When would you use one over a blocking lock?

**Answer**: A spin lock is a synchronization primitive where a thread continuously loops
(spins), checking if the lock is available, instead of being put to sleep. Use a spin lock
when the critical section is **very short** (nanoseconds) and the cost of a context switch
exceeds the cost of spinning. In practice, this is rare in application code — it's more
common in OS kernels and JVM internals. For Java application code, prefer `synchronized`
or `ReentrantLock`, which already use adaptive spinning internally.

---

### Q2: Implement a simple spin lock in Java.

**Answer**:

```java
import java.util.concurrent.atomic.AtomicBoolean;

public class SpinLock {
    private final AtomicBoolean locked = new AtomicBoolean(false);

    public void lock() {
        while (!locked.compareAndSet(false, true)) {
            Thread.onSpinWait();
        }
    }

    public void unlock() {
        locked.set(false);
    }
}
```

Key points: uses `AtomicBoolean.compareAndSet()` (CAS) for atomicity, `Thread.onSpinWait()`
(Java 9+) to hint the CPU for power efficiency, and `set(false)` for release (not CAS —
only the holder should unlock).

---

### Q3: What is the difference between TAS and TTAS spin locks?

**Answer**: **TAS (Test-And-Set)** performs a CAS on every spin iteration. Each CAS is a
write operation that invalidates the cache line on all other cores, causing O(N²) bus
traffic with N competing threads — a "cache coherence storm."

**TTAS (Test-And-Test-And-Set)** adds a read-only inner loop: `while (locked.get()) { spin; }`.
Reads hit the local cache (Shared state in MESI) with zero bus traffic. CAS is attempted
only when the read shows the lock might be free. This reduces bus traffic from O(N²) to
O(N) per lock release, dramatically improving performance under contention.

---

### Q4: What is cache line bouncing? Why does it matter for spin locks?

**Answer**: Cache line bouncing occurs when multiple cores repeatedly write to the same cache
line. Each write invalidates copies on all other cores (MESI protocol), forcing them to
re-fetch the line from the writing core. With N cores doing CAS on a spin lock, each CAS
triggers N-1 invalidations, causing O(N²) bus transactions per round of spinning. This
saturates the memory bus and degrades performance for ALL cores — even the one in the
critical section. TTAS mitigates this by spinning on reads (no invalidation), and CLH/MCS
locks eliminate it by having each thread spin on a different cache line.

---

### Q5: Explain CLH and MCS locks. What problem do they solve?

**Answer**: Both are **queue-based spin locks** that eliminate the thundering herd problem
and cache line bouncing.

**CLH Lock**: Each thread spins on its **predecessor's** node. When the predecessor releases
(sets its flag to false), the successor detects this and enters. FIFO fair, O(1) bus traffic
per release. Disadvantage: on NUMA, spinning on a remote node is slow.

**MCS Lock**: Each thread spins on its **own** node. The predecessor explicitly signals the
successor by writing to the successor's node. NUMA-friendly because each thread spins on
local memory. Used in the Linux kernel's `qspinlock`.

Both provide FIFO fairness and avoid the O(N²) bus traffic of naive spin locks.

---

### Q6: What is adaptive spinning? How does the JVM use it?

**Answer**: Adaptive spinning is a hybrid strategy: spin for a short duration, then block
if the lock isn't acquired. The JVM's HotSpot uses this for `synchronized`:

1. When a thread encounters a contended lock, it spins briefly (not blocking immediately).
2. The spin count adapts based on history: if spinning recently succeeded on this lock,
   spin longer next time; if it recently failed, spin less or skip spinning.
3. If the spin budget is exhausted, the lock is inflated to a heavyweight OS monitor and
   the thread is parked.

This gives the best of both worlds: low latency when contention is brief, CPU efficiency
when contention is sustained.

---

### Q7: Why are spin locks bad on single-core CPUs?

**Answer**: On a single-core CPU, the spinning thread holds the only CPU core. The thread
that holds the lock can't run to release it because the spinner is occupying the core.
The spinner must be preempted by the OS scheduler (time-slice expiry) before the lock
holder can execute and release. This means the spinner wastes an entire time quantum
(typically 1-10ms) accomplishing nothing. Blocking is strictly better: the blocked thread
immediately yields the core, allowing the lock holder to run and release sooner.

---

### Q8: What is Thread.onSpinWait() and why should you use it?

**Answer**: `Thread.onSpinWait()` (Java 9+) is a runtime hint indicating the thread is in a
spin loop. On x86, it compiles to the `PAUSE` instruction, which:
1. **Reduces power consumption** during spinning.
2. **Avoids pipeline penalties** from speculative execution in tight loops.
3. **Improves hyper-threading**: yields execution resources to the sibling logical core.
4. **Near-zero cost** when not spinning.

You should always use it in spin loops. It has no effect on correctness — only performance
and power efficiency. Without it, spin loops can degrade hyper-threading performance by
up to 50% on some architectures.
