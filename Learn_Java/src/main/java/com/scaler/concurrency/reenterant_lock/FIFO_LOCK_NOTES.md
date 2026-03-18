# FIFO Lock — Complete Deep Dive for SDE2

## Table of Contents

1. [What is a FIFO Lock?](#1-what-is-a-fifo-lock)
2. [FIFO Lock = Fair Lock](#2-fifo-lock--fair-lock)
3. [The CLH Queue in AQS](#3-the-clh-queue-in-aqs)
4. [Fair ReentrantLock IS a FIFO Lock](#4-fair-reentrantlock-is-a-fifo-lock)
5. [CLH Queue Deep Dive](#5-clh-queue-deep-dive)
6. [Custom FIFO Lock Implementations](#6-custom-fifo-lock-implementations)
7. [FIFO vs Non-FIFO (Barging) — Visualization](#7-fifo-vs-non-fifo-barging--visualization)
8. [Why FIFO/Fairness Costs Performance](#8-why-fifofairness-costs-performance)
9. [Interview Questions & Answers](#9-interview-questions--answers)

---

## 1. What is a FIFO Lock?

A **FIFO Lock** (First In, First Out) is a lock that guarantees threads acquire the lock
in **exactly the order they requested it**. The thread that requested the lock first will
be the first to acquire it when the lock becomes available.

### Formal Definition

> A lock L is a **FIFO lock** if for any two threads T₁ and T₂, when T₁ calls
> `L.lock()` before T₂ calls `L.lock()`, then T₁ will acquire L before T₂,
> assuming both are contending for the lock.

### The Key Invariant

```
Request order:    T1 → T2 → T3 → T4 → T5
Acquisition order: T1 → T2 → T3 → T4 → T5  (ALWAYS identical)
```

This is in contrast to a **non-FIFO lock** where:
```
Request order:    T1 → T2 → T3 → T4 → T5
Acquisition order: T3 → T1 → T5 → T2 → T4  (any permutation possible)
```

### Why "FIFO"?

The name comes from **queue theory**. A FIFO queue processes elements in the order they
were inserted. A FIFO lock maintains a queue of waiting threads and serves them in
insertion (arrival) order.

---

## 2. FIFO Lock = Fair Lock

**FIFO Lock and Fair Lock are the same concept.** They are two names for the same property:
threads acquire the lock in the order they requested it.

| Term          | Meaning                                    | Used In                    |
|---------------|--------------------------------------------|----------------------------|
| FIFO Lock     | Acquisition order = request order           | Academic literature, OS    |
| Fair Lock     | No thread is unfairly skipped               | Java documentation, JUC    |
| Ordered Lock  | Lock maintains an ordering invariant        | Distributed systems        |
| Queued Lock   | Lock backed by a waiting queue              | Spinlock literature        |

In Java:
```java
ReentrantLock fifoLock = new ReentrantLock(true);  // This IS a FIFO lock
```

When the Java docs say "fair," they mean FIFO. When academic papers say "FIFO lock," they
mean fair. The terms are interchangeable.

### Subtle Distinction

Some implementations use the term "FIFO" specifically for **spinlocks** (where threads
actively spin rather than park), while "fair" is used for **blocking locks** (where threads
are parked/slept). But the ordering guarantee is identical.

---

## 3. The CLH Queue in AQS

Java's `AbstractQueuedSynchronizer` (AQS) — the backbone of `ReentrantLock`, `Semaphore`,
`CountDownLatch`, and more — uses a variant of the **CLH Lock queue** to manage waiting
threads.

### What is CLH?

**CLH** stands for **Craig, Landin, and Hagersten** — the three researchers who invented
this lock queue algorithm. The original CLH lock is a spinlock where each thread spins on
its **predecessor's node** rather than on a shared variable.

### AQS's Modified CLH Queue

AQS uses a modified CLH queue that is:
- **Doubly linked** (original CLH is singly linked) — needed for cancellation
- **Blocking** (uses `LockSupport.park()`) instead of spinning
- **Supports multiple modes** (exclusive and shared)

```
AQS CLH Queue Structure:

  HEAD                                                    TAIL
   ↓                                                       ↓
┌──────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ Node │←──→│  Node    │←──→│  Node    │←──→│  Node    │
│(dummy)│   │ thread=T1│    │ thread=T2│    │ thread=T3│
│ ws=0  │   │ ws=SIGNAL│    │ ws=SIGNAL│    │ ws=0     │
└──────┘    └──────────┘    └──────────┘    └──────────┘

HEAD is a dummy sentinel node (no thread associated with it).
The actual first waiter is HEAD.next (T1 in this diagram).
New threads are added at the TAIL.
```

### How FIFO Ordering is Enforced

1. Thread calls `lock()` → `tryAcquire()` fails
2. Thread creates a `Node`, adds itself to the **tail** of the queue via CAS
3. Thread checks: "Am I the head's successor?"
   - If YES → try `tryAcquire()` again
   - If NO → `LockSupport.park(this)` — go to sleep
4. When lock is released:
   - Releasing thread sets `state = 0`
   - Releasing thread calls `unparkSuccessor(head)` — wakes up head's next node
5. The woken thread (head's successor) tries `tryAcquire()`
   - In fair mode: succeeds (it IS the first waiter)
   - In non-fair mode: might lose to a barging thread

Because threads are always added at the tail and removed from the head, the queue is
strictly FIFO.

---

## 4. Fair ReentrantLock IS a FIFO Lock

This is the direct relationship:

```java
// A FIFO lock in Java is simply:
Lock fifoLock = new ReentrantLock(true);  // fair = true → FIFO ordering guaranteed
```

### Proof: Fair Lock Enforces FIFO

The fair lock's `tryAcquire()` has this critical check:

```java
// Inside FairSync.tryAcquire()
if (getState() == 0) {
    if (!hasQueuedPredecessors() && compareAndSetState(0, 1)) {
        setExclusiveOwnerThread(Thread.currentThread());
        return true;
    }
}
```

`hasQueuedPredecessors()` returns `true` if there's anyone in the queue ahead of the
current thread. If so, the thread **cannot** acquire the lock, even if it's free. It must
join the queue and wait its turn.

This is exactly the FIFO property: **you cannot skip ahead of earlier arrivals**.

### Non-Fair Lock is NOT a FIFO Lock

```java
// Inside NonfairSync.tryAcquire()
if (getState() == 0) {
    if (compareAndSetState(0, 1)) {  // NO hasQueuedPredecessors() check!
        setExclusiveOwnerThread(Thread.currentThread());
        return true;
    }
}
```

No queue check → barging allowed → not FIFO.

---

## 5. CLH Queue Deep Dive

### Node Structure

Each node in the AQS CLH queue contains:

```java
static final class Node {
    volatile int waitStatus;     // SIGNAL, CANCELLED, CONDITION, PROPAGATE, or 0
    volatile Node prev;          // link to predecessor
    volatile Node next;          // link to successor
    volatile Thread thread;      // the thread this node represents
    Node nextWaiter;             // link used in Condition queues
}
```

### Node waitStatus Values

| Value       | Constant   | Meaning                                                    |
|-------------|------------|------------------------------------------------------------|
| `0`         | (default)  | Initial state when node is created                         |
| `-1`        | `SIGNAL`   | This node's successor is (or will be) parked. When this    |
|             |            | node releases or cancels, it must unpark its successor.    |
| `1`         | `CANCELLED`| Thread gave up waiting (timeout or interrupt). Node will   |
|             |            | be removed from queue. Only positive value.                |
| `-2`        | `CONDITION`| Node is in a Condition queue, not the sync queue. Will     |
|             |            | be transferred to sync queue when signaled.                |
| `-3`        | `PROPAGATE`| Used only for shared mode (e.g., Semaphore). A             |
|             |            | releaseShared should propagate to subsequent nodes.        |

### Lifecycle of a Node in the Queue

```
1. Thread fails tryAcquire()
         ↓
2. Node created (waitStatus = 0)
         ↓
3. CAS: add node to tail of queue
         ↓
4. Set predecessor's waitStatus to SIGNAL
   (predecessor promises to wake us up when it releases)
         ↓
5. LockSupport.park(this) — thread sleeps
         ↓
   ... waiting ...
         ↓
6. Predecessor releases lock → calls unparkSuccessor()
         ↓
7. Thread wakes up → tryAcquire() → succeeds
         ↓
8. Node becomes the new HEAD (old head is GC'd)
```

### Enqueue Operation (addWaiter)

```java
// Simplified pseudocode
private Node addWaiter(Node mode) {
    Node node = new Node(Thread.currentThread(), mode);
    Node pred = tail;
    if (pred != null) {
        node.prev = pred;
        if (compareAndSetTail(pred, node)) {   // CAS tail to new node
            pred.next = node;
            return node;
        }
    }
    enq(node);  // full enqueue (initializes queue if needed)
    return node;
}
```

### Dequeue Operation (after acquiring lock)

```java
// Simplified pseudocode
private void setHead(Node node) {
    head = node;
    node.thread = null;   // head is always a dummy node
    node.prev = null;
}
```

The old head is disconnected and garbage collected. The acquiring node becomes the new
dummy head.

### Why Doubly Linked?

The original CLH lock uses a singly linked list (each node points to its predecessor).
AQS needs doubly linked because:

1. **Cancellation**: When a node is cancelled, we need to update `predecessor.next` to
   skip the cancelled node. This requires traversing from the predecessor forward.
2. **Unpaking successor**: When releasing the lock, we need to find `head.next` to know
   which thread to unpark.
3. **`hasQueuedPredecessors()`**: Fair lock needs to check if `head.next.thread` is the
   current thread, requiring forward links.

---

## 6. Custom FIFO Lock Implementations

Beyond Java's `ReentrantLock(true)`, there are classic FIFO lock algorithms. Understanding
them is valuable for interviews and systems design.

### 6.1 Ticket Lock

The simplest FIFO lock. Works like a deli counter: take a number, wait for your number
to be called.

```java
import java.util.concurrent.atomic.AtomicInteger;

public class TicketLock {
    private final AtomicInteger ticketCounter = new AtomicInteger(0);  // next ticket to issue
    private final AtomicInteger servingCounter = new AtomicInteger(0); // currently serving

    public void lock() {
        int myTicket = ticketCounter.getAndIncrement();  // take a number
        while (servingCounter.get() != myTicket) {       // spin until my number is called
            Thread.onSpinWait();
        }
    }

    public void unlock() {
        servingCounter.incrementAndGet();  // call the next number
    }
}
```

**Properties:**
- Strictly FIFO (ticket order = acquisition order)
- Simple to implement
- **Problem**: all threads spin on the same `servingCounter` → cache line bouncing
  (high cache coherence traffic on multi-core)

### 6.2 CLH Lock (Spinlock Version)

Each thread spins on its **predecessor's node** instead of a shared variable.

```java
import java.util.concurrent.atomic.AtomicReference;

public class CLHLock {
    private final AtomicReference<Node> tail = new AtomicReference<>(new Node(false));
    private final ThreadLocal<Node> myNode = ThreadLocal.withInitial(() -> new Node(true));
    private final ThreadLocal<Node> myPred = new ThreadLocal<>();

    public void lock() {
        Node node = myNode.get();
        node.locked = true;
        Node pred = tail.getAndSet(node);   // atomically add to tail
        myPred.set(pred);
        while (pred.locked) {               // spin on predecessor
            Thread.onSpinWait();
        }
    }

    public void unlock() {
        myNode.get().locked = false;        // signal successor
        myNode.set(myPred.get());           // reuse predecessor's node
    }

    private static class Node {
        volatile boolean locked;
        Node(boolean locked) { this.locked = locked; }
    }
}
```

**Properties:**
- FIFO ordering
- Each thread spins on a **different** cache line (predecessor's node)
- Better than TicketLock on NUMA architectures
- **Problem**: on NUMA, the predecessor's node may be in remote memory → slow spin

### 6.3 MCS Lock

**MCS** (Mellor-Crummey, Scott) fixes CLH's NUMA problem. Each thread spins on its
**own** node (local to its processor).

```java
import java.util.concurrent.atomic.AtomicReference;

public class MCSLock {
    private final AtomicReference<Node> tail = new AtomicReference<>(null);
    private final ThreadLocal<Node> myNode = ThreadLocal.withInitial(Node::new);

    public void lock() {
        Node node = myNode.get();
        node.locked = true;
        Node pred = tail.getAndSet(node);
        if (pred != null) {
            pred.next = node;               // tell predecessor about us
            while (node.locked) {           // spin on OWN node (local memory)
                Thread.onSpinWait();
            }
        }
    }

    public void unlock() {
        Node node = myNode.get();
        if (node.next == null) {
            if (tail.compareAndSet(node, null)) {
                return;                     // no successor, queue is empty
            }
            while (node.next == null) {     // wait for successor to link
                Thread.onSpinWait();
            }
        }
        node.next.locked = false;           // signal successor
        node.next = null;
    }

    private static class Node {
        volatile boolean locked = false;
        volatile Node next = null;
    }
}
```

**Properties:**
- FIFO ordering
- Each thread spins on its **own** node → NUMA-friendly
- More complex than CLH
- Used in Linux kernel (`qspinlock` is MCS-based)

### Comparison Table

| Lock          | FIFO? | Spin Location           | NUMA-Friendly? | Complexity |
|---------------|-------|-------------------------|----------------|------------|
| Ticket Lock   | Yes   | Shared counter          | No             | Simple     |
| CLH Lock      | Yes   | Predecessor's node      | Partial        | Medium     |
| MCS Lock      | Yes   | Own node                | Yes            | Complex    |
| AQS Fair Lock | Yes   | Parks (doesn't spin)    | N/A (blocking) | Medium     |

---

## 7. FIFO vs Non-FIFO (Barging) — Visualization

### Scenario: 5 threads arrive in order T1, T2, T3, T4, T5

#### FIFO Lock (Fair ReentrantLock)

```
Time →
          acquire    release
T1:  ─────[████████]────────────────────────────────────────
T2:  ───────wait────[████████]──────────────────────────────
T3:  ─────────wait──────wait──[████████]────────────────────
T4:  ───────────wait──────wait────wait──[████████]──────────
T5:  ─────────────wait──────wait────wait────wait──[████████]

Acquisition order: T1 → T2 → T3 → T4 → T5 (ALWAYS)
Every thread waits for all predecessors. Predictable but slow.
```

#### Non-FIFO Lock (Non-Fair ReentrantLock)

```
Time →
          acquire    release
T1:  ─────[████████]────────────────────────────────────────
T3:  ─────────wait──[████████]──── ← BARGING! T3 was running and stole lock
T5:  ───────────wait──[████████]── ← BARGING! T5 stole it too
T2:  ───────wait──────wait────wait──[████████]──────────────
T4:  ─────────wait──────wait──────wait──wait──[████████]────

Acquisition order: T1 → T3 → T5 → T2 → T4 (unpredictable)
Threads that happen to be running on a CPU can barge in.
Higher throughput because fewer context switches.
```

### Why the Difference Matters

```
FIFO Lock (Fair):
  ✅ Predictable latency per thread
  ✅ No starvation
  ✅ Order guarantees
  ❌ Forced context switches → lower throughput
  ❌ Convoy effect → all threads go at the speed of the slowest

Non-FIFO Lock (Non-Fair):
  ✅ Higher throughput (fewer context switches)
  ✅ Better CPU utilization
  ❌ Unpredictable per-thread latency
  ❌ Theoretical starvation risk
  ❌ No ordering guarantee
```

---

## 8. Why FIFO/Fairness Costs Performance

### Cost #1: Forced Context Switching

When a FIFO lock is released, it **must** wake up the next thread in the queue, even if
another thread is currently running and could immediately use the lock.

```
Context switch cost breakdown:
  - Save current thread state    ~0.5 μs
  - OS scheduler decision        ~0.5 μs
  - Restore new thread state     ~0.5 μs
  - Cache warming (L1/L2 miss)   ~1-5 μs
  - TLB flush                    ~0.5-1 μs
  ─────────────────────────────────────
  Total per context switch:       ~3-8 μs

With non-fair lock: running thread grabs lock instantly (~10 ns CAS)
With fair lock: forced context switch (~3,000-8,000 ns)
That's 300-800x slower per acquisition!
```

### Cost #2: The Convoy Effect

Threads form a "convoy" behind the lock. Each acquisition requires waking the next thread,
doing work, then waking the next thread, and so on. If any thread in the convoy is slow,
all subsequent threads are delayed.

### Cost #3: Queue Maintenance Overhead

Fair locks must always check `hasQueuedPredecessors()` before CAS. This is an additional
volatile read of `head` and `tail` pointers, plus checking `head.next.thread`. Under
heavy contention, these reads cause cache line bouncing.

### Cost #4: Reduced Batching

Non-fair locks naturally "batch" operations: a running thread might acquire and release
the lock multiple times before a parked thread wakes up. This is efficient because the
thread's working set stays hot in cache. Fair locks prevent this batching.

### When the Cost Doesn't Matter

- **Low contention**: If threads rarely compete, fairness overhead is negligible
- **Long critical sections**: If the work inside the lock takes milliseconds, the
  microsecond overhead of fairness is irrelevant
- **Correctness requirements**: When FIFO ordering is a correctness property, not
  just a performance preference

---

## 9. Interview Questions & Answers

### Q1: What is a FIFO lock and how does it relate to a fair lock?

**A:** A FIFO lock guarantees that threads acquire the lock in the order they requested
it — first in, first out. In Java, a "FIFO lock" and a "fair lock" are the same concept.
`new ReentrantLock(true)` creates a FIFO/fair lock that uses AQS's CLH queue to maintain
request ordering. The terms are interchangeable: "FIFO" is used more in academic/OS
literature, "fair" is the Java terminology.

---

### Q2: Explain how AQS's CLH queue enforces FIFO ordering.

**A:** AQS maintains a doubly linked CLH queue. When a thread fails to acquire the lock,
it creates a Node and atomically appends it to the queue tail via CAS. When the lock is
released, AQS unparks the node after the head (the longest-waiting thread). In fair mode,
`tryAcquire()` calls `hasQueuedPredecessors()` to ensure no thread skips the queue. Since
threads are always added at the tail and served from the head, the queue is strictly FIFO.

---

### Q3: What are the waitStatus values in an AQS Node and what do they mean?

**A:** There are five values: (1) `0` — initial state; (2) `SIGNAL (-1)` — this node
must unpark its successor when it releases; (3) `CANCELLED (1)` — thread timed out or
was interrupted, node will be removed; (4) `CONDITION (-2)` — node is in a Condition
queue, not the sync queue; (5) `PROPAGATE (-3)` — used in shared mode to propagate
release signals. Only `CANCELLED` is positive, which is used to detect cancelled nodes
when traversing the queue.

---

### Q4: Compare Ticket Lock, CLH Lock, and MCS Lock.

**A:** All three are FIFO spinlocks. **Ticket Lock** uses two atomic counters (ticket
and serving number) — simple but all threads spin on the same counter causing cache
bouncing. **CLH Lock** has each thread spin on its predecessor's node — better cache
behavior since each thread reads a different cache line, but not optimal on NUMA systems
because the predecessor's node may be in remote memory. **MCS Lock** has each thread
spin on its own node — optimal for NUMA since the spin variable is always local. MCS is
the most complex but most scalable; it's used in the Linux kernel's `qspinlock`.

---

### Q5: Why doesn't Java's AQS use a spinlock-based CLH approach?

**A:** AQS uses a **blocking** CLH queue (threads are parked with `LockSupport.park()`)
rather than spinning because: (1) spinning wastes CPU cycles — in a general-purpose JVM
with many threads, burning a CPU core to spin is wasteful; (2) parking allows the OS
scheduler to use the CPU for other threads; (3) most Java locks protect critical sections
that take longer than a spin-wait would justify; (4) AQS does do a brief spin
(`shouldParkAfterFailedAcquire` retries before parking) as a compromise.

---

### Q6: If fair ReentrantLock is already FIFO, why would you implement a custom TicketLock?

**A:** Custom spinlock implementations like TicketLock are useful in scenarios where: (1)
blocking overhead is too high (the critical section is extremely short, <1 μs); (2) you
need a lock in a real-time or low-latency system where `LockSupport.park()` latency is
unacceptable; (3) you're building a lock for a specific hardware architecture (NUMA) where
MCS is optimal; (4) you need a lock without depending on `java.util.concurrent`. However,
for most Java applications, `ReentrantLock(true)` is the correct choice for a FIFO lock.

---

### Q7: What is the convoy effect and why is it worse with FIFO locks?

**A:** The convoy effect occurs when threads form a "convoy" behind a lock, each waiting
for the previous one to complete. With FIFO locks, this is enforced — even if Thread-N
could execute immediately because it's already on a CPU, it must wait for threads 1 through
N-1 to each acquire, execute, and release. Each handoff requires a context switch. With
non-FIFO locks, a running thread can bypass the convoy by barging, reducing the chain of
forced context switches. The convoy effect is the primary reason fair locks have lower
throughput.

---

### Q8: How would you implement a FIFO lock using only AtomicInteger?

**A:** A TicketLock using two `AtomicInteger` values: `ticketCounter` (the next ticket to
issue) and `servingCounter` (the currently serving number). To lock: atomically increment
`ticketCounter` to get your ticket, then spin-wait until `servingCounter` equals your
ticket. To unlock: increment `servingCounter`. This guarantees FIFO because tickets are
sequential and served in order. The downside is cache contention on `servingCounter` since
all threads spin-read it.
