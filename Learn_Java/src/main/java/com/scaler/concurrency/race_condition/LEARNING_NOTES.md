# Race Conditions — SDE2 Interview Preparation

## Table of Contents
1. [What is a Race Condition](#1-what-is-a-race-condition)
2. [Check-Then-Act Race Condition](#2-check-then-act-race-condition)
3. [Read-Modify-Write Race Condition](#3-read-modify-write-race-condition)
4. [Why counter++ is Not Atomic](#4-why-counter-is-not-atomic)
5. [Data Race vs Race Condition](#5-data-race-vs-race-condition)
6. [Real-World Consequences](#6-real-world-consequences)
7. [How to Detect Race Conditions](#7-how-to-detect-race-conditions)
8. [How to Fix Race Conditions](#8-how-to-fix-race-conditions)
9. [The Java Memory Model Angle](#9-the-java-memory-model-angle)
10. [Time-of-Check to Time-of-Use (TOCTOU)](#10-time-of-check-to-time-of-use-toctou)
11. [SDE2 Interview Q&A](#11-sde2-interview-qa)

---

## 1. What is a Race Condition

### Formal Definition

A **race condition** occurs when the correctness of a program depends on the **relative timing
or interleaving** of operations by multiple threads. The outcome varies depending on which
thread "wins the race."

More precisely: a race condition exists when a program's behavior depends on the non-deterministic
scheduling of threads, and at least one possible interleaving produces an incorrect result.

### Key Insight

Race conditions are **not** about speed — they are about **ordering**. Even on a single-core
CPU (where threads truly take turns), race conditions can occur because the scheduler can
preempt a thread between any two instructions.

### Categories

| Type                  | Pattern                           | Example                     |
|-----------------------|-----------------------------------|-----------------------------|
| **Check-then-act**    | Test a condition, then act on it  | Lazy initialization         |
| **Read-modify-write** | Read a value, modify it, write back | `counter++`              |

Both patterns fail because the compound action is not **atomic** — another thread can
intervene between the sub-operations.

---

## 2. Check-Then-Act Race Condition

### The Pattern

```java
if (condition) {          // CHECK
    doSomething();        // ACT
}
```

Between the CHECK and the ACT, another thread may change the state, invalidating the
condition. The acting thread proceeds on a stale assumption.

### Classic Example: Lazy Singleton (Broken)

```java
public class Singleton {
    private static Singleton instance;

    public static Singleton getInstance() {
        if (instance == null) {          // Thread A checks: null? Yes.
            // Thread B also checks: null? Yes.
            instance = new Singleton();   // Both create instances!
        }
        return instance;
    }
}
```

**Interleaving**:
1. Thread A: `instance == null` → true
2. Thread B: `instance == null` → true (A hasn't written yet)
3. Thread A: `instance = new Singleton()` → assigns
4. Thread B: `instance = new Singleton()` → overwrites with second instance

Two Singleton instances are created — violating the singleton invariant.

### Another Example: Map "Put If Absent"

```java
if (!map.containsKey(key)) {     // CHECK
    map.put(key, computeValue()); // ACT — another thread may have inserted between check and put
}
```

This is why `ConcurrentHashMap.putIfAbsent()` exists — it performs the check-then-act
atomically.

---

## 3. Read-Modify-Write Race Condition

### The Pattern

```java
counter++;   // Looks like one operation, but it's three
```

This is syntactic sugar for:
```
1. READ:   temp = counter;       // read current value
2. MODIFY: temp = temp + 1;      // increment in register
3. WRITE:  counter = temp;        // write back
```

### The Interleaving Problem

```
Thread A                    Thread B
─────────                   ─────────
READ counter (= 0)
                            READ counter (= 0)
MODIFY: 0 + 1 = 1
                            MODIFY: 0 + 1 = 1
WRITE counter = 1
                            WRITE counter = 1

Final counter: 1 (should be 2!)
```

This is called a **lost update** — Thread B's write overwrites Thread A's increment.

### How Bad Can It Get?

With 2 threads each doing 100,000 increments:
- **Expected**: 200,000
- **Observed**: Anywhere from ~100,000 to 200,000

The closer to 200,000, the "luckier" the scheduling was. But the code is **always broken**
— you just might not observe the bug on every run.

---

## 4. Why counter++ is Not Atomic

### Bytecode Proof

The Java compiler emits the following bytecodes for `counter++` on an instance field:

```
aload_0               // push 'this' onto stack
dup                   // duplicate 'this' (need it twice)
getfield #2           // READ: push counter's value
iconst_1              // push constant 1
iadd                  // MODIFY: add
putfield #2           // WRITE: store back
```

A thread can be preempted (context-switched) between **any** of these bytecodes. Even if
the thread is not preempted, on a multi-core system, another core may execute the same
sequence simultaneously, both reading the same value.

### Even `volatile` Doesn't Help

```java
private volatile int counter = 0;
counter++;  // STILL NOT ATOMIC
```

`volatile` guarantees visibility (each read goes to main memory, each write is flushed),
but the three-step read-modify-write is still non-atomic. Between the volatile read and
the volatile write, another thread can perform its own read-modify-write.

### What IS Atomic in Java?

- Reads and writes of reference types (32-bit on 32-bit JVM, always on 64-bit)
- Reads and writes of primitive types ≤ 32 bits (`int`, `float`, `char`, `short`, `byte`, `boolean`)
- Reads and writes of `volatile long` and `volatile double` (the JMM guarantees this)
- Operations on `java.util.concurrent.atomic.*` classes

---

## 5. Data Race vs Race Condition

These terms are often used interchangeably but have distinct technical meanings.

### Data Race (JMM Definition)

A **data race** occurs when:
1. Two threads access the same memory location.
2. At least one access is a write.
3. There is **no happens-before relationship** ordering the accesses.

A data race is a **JMM-level** concept. It means the program has undefined behavior under
the Java Memory Model. The JMM provides no guarantees about what values will be seen.

### Race Condition (Logic-Level)

A **race condition** is a **semantic** bug — the program produces incorrect results due to
non-deterministic thread scheduling, even though there may be happens-before relationships.

### The Subtle Difference

You can have a race condition **without** a data race:

```java
// No data race (all accesses are synchronized), but still a race condition:
synchronized (lock) { if (!list.contains(x)) }
// another thread adds x between the two synchronized blocks
synchronized (lock) { list.add(x); }  // adds duplicate!
```

Both accesses are properly synchronized (no data race), but the check-then-act is not
atomic, so the logic is incorrect (race condition).

Conversely, you can have a data race **without** a (harmful) race condition:
```java
// Data race (no sync), but the program doesn't depend on the value:
sharedFlag = true;  // data race, but if we don't care about exact timing, it's "benign"
```

### Interview Tip

Always clarify which one you're talking about. Saying "data race" when you mean "race
condition" (or vice versa) shows imprecise understanding.

| Aspect           | Data Race                        | Race Condition                 |
|------------------|----------------------------------|--------------------------------|
| Level            | Memory model / hardware          | Application logic              |
| Definition       | Unsynchronized concurrent access | Correctness depends on timing  |
| Can exist alone? | Yes (benign data race)           | Yes (without data race)        |
| Fix              | Establish happens-before         | Make compound action atomic    |

---

## 6. Real-World Consequences

### 6.1 Banking — Double Debit

```
Account balance: $1000
Thread A: withdraw($800) — reads $1000, checks $1000 >= $800
Thread B: withdraw($800) — reads $1000, checks $1000 >= $800
Thread A: writes $200
Thread B: writes $200 (overwrites, or worse, if computed independently: $200)
```

Both withdrawals succeed. Customer withdrew $1600 from a $1000 account.

### 6.2 Inventory Management — Overselling

```
Stock: 1 item left
Thread A (User 1): reads stock = 1, stock > 0 → proceed to checkout
Thread B (User 2): reads stock = 1, stock > 0 → proceed to checkout
Both complete purchase. Stock should be -1.
```

Two customers bought the same last item. One order must be cancelled — bad UX and
potential legal issues.

### 6.3 Ticket Booking — Double Booking

Same pattern as inventory. Seat 42A is assigned to two passengers. One shows up and finds
their seat taken.

### 6.4 Therac-25 (Real Incident)

The Therac-25 radiation therapy machine had a race condition between the operator console
and the treatment subsystem. The race caused patients to receive lethal radiation doses.
Three people died. This is the most cited real-world example of fatal race conditions.

### 6.5 Mars Pathfinder (1997)

A priority inversion (caused by a race condition pattern) repeatedly rebooted the Sojourner
rover. NASA diagnosed and patched it remotely by enabling priority inheritance on the mutex.

---

## 7. How to Detect Race Conditions

### 7.1 Code Review

Manually identify shared mutable state and check if all access paths are properly
synchronized. Look for:
- Fields accessed by multiple threads without `synchronized`, `volatile`, or `Atomic*`.
- Compound actions (check-then-act, read-modify-write) that are not atomic.
- Inconsistent lock ordering.

### 7.2 Stress Testing

Run multithreaded tests with many threads and many iterations. Race conditions are
probabilistic — more iterations increase the chance of triggering them.

```java
@RepeatedTest(1000)
void testCounter() {
    // run concurrent increments, assert final value
}
```

### 7.3 Thread Sanitizer (TSan)

Google's ThreadSanitizer (integrated into GCC and Clang for C/C++) detects data races at
runtime with ~5-15x slowdown. For Java:
- **jcstress** (OpenJDK concurrency stress testing harness) is the closest equivalent.
- It systematically explores thread interleavings.

### 7.4 Static Analysis Tools

- **SpotBugs / FindBugs**: Detects patterns like inconsistent synchronization.
- **IntelliJ IDEA inspections**: Flags shared mutable state without proper synchronization.
- **Error Prone** (Google): Compile-time bug detection for Java.

### 7.5 Dynamic Analysis

- **Java Flight Recorder (JFR)** + **JDK Mission Control**: Record lock contention events.
- Insert thread-local assertions that verify invariants under concurrent execution.

### 7.6 Formal Verification

For critical systems, use model checkers like **Java PathFinder (JPF)** that systematically
explore all possible thread interleavings.

---

## 8. How to Fix Race Conditions

### 8.1 `synchronized` Keyword

```java
private int counter = 0;
private final Object lock = new Object();

public void increment() {
    synchronized (lock) {
        counter++;
    }
}
```

Pros: Simple, well-understood, reentrant.
Cons: Can cause contention, potential deadlocks, no tryLock/timeout.

### 8.2 Atomic Classes (`java.util.concurrent.atomic`)

```java
private final AtomicInteger counter = new AtomicInteger(0);

public void increment() {
    counter.incrementAndGet();  // atomic CAS-based, lock-free
}
```

Internally uses `compareAndSwapInt` (CAS) in a retry loop:
```
do {
    current = get();
    next = current + 1;
} while (!compareAndSet(current, next));
```

Pros: Lock-free, no deadlocks, excellent performance under moderate contention.
Cons: Only works for single variables; can't protect compound actions on multiple fields.

### 8.3 Explicit Locks (`java.util.concurrent.locks`)

```java
private final ReentrantLock lock = new ReentrantLock();

public void increment() {
    lock.lock();
    try {
        counter++;
    } finally {
        lock.unlock();
    }
}
```

Pros: tryLock with timeout, fair/unfair modes, interruptible, condition variables.
Cons: More verbose, must remember try/finally, not auto-released.

### 8.4 Concurrent Collections

Replace `HashMap` with `ConcurrentHashMap`, `ArrayList` with `CopyOnWriteArrayList`, etc.
These classes internalize the synchronization so callers don't need external locks.

```java
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
map.merge(key, 1, Integer::sum);  // atomic merge
```

### 8.5 Immutability

If data cannot be changed after construction, it cannot have race conditions. Prefer
immutable objects (`final` fields, no setters, defensive copies).

```java
public final class ImmutablePoint {
    private final int x;
    private final int y;

    public ImmutablePoint(int x, int y) {
        this.x = x;
        this.y = y;
    }
    // getters only, no setters
}
```

### 8.6 Thread Confinement

If only one thread ever accesses the data, there is no race condition.
- **Stack confinement**: Use local variables.
- **ThreadLocal**: Each thread gets its own copy.
- **Single-threaded subsystems**: Process all work on one thread (event loops, Swing EDT).

---

## 9. The Java Memory Model Angle

### Visibility vs Atomicity

The JMM distinguishes two orthogonal concerns:

**Visibility**: When Thread A writes `x = 42`, will Thread B see 42 when it reads `x`?
Without a happens-before relationship, the answer is: **not guaranteed**.

**Atomicity**: Is the operation indivisible? Can another thread observe an intermediate state?

| Problem              | Visibility issue?  | Atomicity issue? |
|----------------------|-------------------|------------------|
| `counter++`          | Yes               | Yes              |
| Reading stale flag   | Yes               | No               |
| Writing `long` (32-bit JVM) | No (usually) | Yes (word tearing) |

### Happens-Before and Race Conditions

Even if you fix visibility (e.g., with `volatile`), you may still have a race condition
if the operation is not atomic:

```java
private volatile int counter = 0;

// Thread A and Thread B both call:
counter++;  // volatile ensures visibility but NOT atomicity!
```

Both threads see the latest value (visibility is fine), but the read-modify-write is still
three steps, so they can both read the same value, increment it, and write the same result.

### The Full Fix

You need **both** visibility **and** atomicity:
- `synchronized` provides both.
- `AtomicInteger` provides both (via CAS).
- `volatile` provides only visibility.

---

## 10. Time-of-Check to Time-of-Use (TOCTOU)

### Definition

A TOCTOU race condition occurs when the state checked by a program changes between the
time of the check and the time the check result is used.

### Classic File System Example (Not Java-specific)

```
if (file.exists()) {           // TIME OF CHECK
    file.delete();              // TIME OF USE — file may have been deleted by another process
}
```

Between `exists()` and `delete()`, another process may delete the file, move it, or change
its permissions.

### Java Examples

**Lazy initialization (already covered):**
```java
if (instance == null) {        // CHECK
    instance = new Foo();       // USE — another thread may have set it
}
```

**Collection size check:**
```java
if (list.size() > 0) {        // CHECK
    return list.get(0);         // USE — another thread may have removed elements
}
```

**Map operations:**
```java
if (map.containsKey(key)) {   // CHECK
    return map.get(key);        // USE — another thread may have removed the key
}
```

### Why TOCTOU is Particularly Insidious

- The check and the action are often in different methods or even different classes.
- The race window may be very small (nanoseconds), making it hard to reproduce.
- The bug may only manifest under specific timing conditions (high load, GC pause, etc.).

### Fixes

- **Atomic operations**: `putIfAbsent()`, `computeIfAbsent()`, `getOrDefault()`.
- **Synchronized blocks**: Wrap both check and action in one `synchronized` block.
- **Lock-free algorithms**: Use CAS to atomically check-and-update.

---

## 11. SDE2 Interview Q&A

### Q1: What is a race condition? Give a real-world example.

**A**: A race condition occurs when the correctness of a program depends on the relative
timing of thread execution. A real-world example is a bank account: two ATM machines
simultaneously process withdrawals. Both read the same balance ($1000), both verify the
withdrawal amount ($800) is valid, and both proceed. The account ends up with $200 instead
of the correct -$600 (which should have been rejected). This happens because the
check-balance and deduct-balance operations are not atomic.

### Q2: Explain the difference between a data race and a race condition.

**A**: A **data race** is a JMM-level concept: two threads access the same variable with no
happens-before ordering, and at least one is a write. A **race condition** is a logic-level
bug: program correctness depends on scheduling order. You can have a race condition without
a data race (e.g., check-then-act where each individual access is synchronized, but the
compound operation is not atomic). You can also have a data race without a harmful race
condition (benign data race on a flag that is eventually consistent).

### Q3: Why is `counter++` not thread-safe? What happens at the bytecode level?

**A**: `counter++` compiles to `getfield`, `iconst_1`, `iadd`, `putfield`. It's a
read-modify-write: the thread reads the current value, increments it locally, then writes
back. Another thread can read the same original value between the read and write, causing a
lost update. Even `volatile` doesn't fix this because it only ensures visibility, not
atomicity of the compound operation.

### Q4: What is a check-then-act race condition? How do you fix it?

**A**: It's when a thread checks a condition and then takes action based on it, but the
condition may have changed between the check and the action. For example, checking if a key
exists in a map and then inserting — another thread may insert between the check and the
insert. Fix by making the check-and-act atomic: use `ConcurrentHashMap.putIfAbsent()`,
wrap both in a `synchronized` block, or use a CAS loop.

### Q5: How would you detect race conditions in a large codebase?

**A**: Multiple approaches:
1. **Code review**: Look for shared mutable state accessed without synchronization.
2. **Static analysis**: SpotBugs/FindBugs can detect inconsistent synchronization patterns.
3. **Stress testing**: Run concurrent tests with many threads and iterations (jcstress).
4. **Dynamic analysis**: JFR to record lock contention; thread dumps to find suspicious states.
5. **Formal methods**: Java PathFinder for systematic interleaving exploration (for critical code).

### Q6: Explain TOCTOU. Give a Java example and its fix.

**A**: TOCTOU (Time-of-Check to Time-of-Use) is when the condition checked by a program
changes between the check and its use. Example: `if (map.containsKey(k)) return map.get(k);`
— between `containsKey` and `get`, another thread may remove the key, causing `get` to return
null. Fix: use `map.getOrDefault(k, default)` or `map.computeIfAbsent(k, func)` which
perform the check-and-act atomically.

### Q7: Can volatile fix a race condition on `counter++`? Why or why not?

**A**: No. `volatile` guarantees that reads see the latest write (visibility) and prevents
reordering (ordering), but `counter++` is a compound operation (read, increment, write).
Between the volatile read and the volatile write, another thread can perform its own
read-increment-write. The fix requires atomicity: use `AtomicInteger.incrementAndGet()`
(CAS-based) or wrap in `synchronized`.

### Q8: What is the Therac-25 incident and what does it teach about race conditions?

**A**: The Therac-25 was a radiation therapy machine in the 1980s. A race condition between
the operator console and the beam control subsystem allowed the machine to deliver lethal
radiation doses. Three patients died. The lesson: race conditions in safety-critical systems
are not just bugs — they can be fatal. Concurrent code in such systems requires formal
verification, not just testing.

### Q9: How does `AtomicInteger.incrementAndGet()` work internally?

**A**: It uses a CAS (Compare-And-Swap) loop:
1. Read the current value.
2. Compute the new value (current + 1).
3. Attempt `compareAndSwap(current, new)`. This is a single atomic CPU instruction.
4. If CAS succeeds (no one else changed it), done.
5. If CAS fails (another thread changed it), go back to step 1.
This is **lock-free** — no thread is ever blocked. Under high contention, threads may retry
many times, but there's always global progress (at least one thread succeeds per CAS round).

### Q10: How does immutability prevent race conditions?

**A**: An immutable object's state cannot change after construction. Since race conditions
require shared **mutable** state, immutable objects are inherently thread-safe. In Java, make
fields `final`, provide no setters, and ensure the object is safely published (constructor
doesn't leak `this`). Examples: `String`, `Integer`, `LocalDate`. For complex state, create
new instances instead of modifying existing ones (functional style).

---

## Quick Reference Cheat Sheet

```
┌──────────────────────────────────────────────────────────────────┐
│  RACE CONDITION TYPES                                            │
│                                                                  │
│  Check-then-act: if (cond) { act(); }   // cond may change      │
│  Read-modify-write: counter++           // 3 non-atomic steps    │
│  TOCTOU: check(x) ... use(x)           // x may change          │
│                                                                  │
│  DATA RACE vs RACE CONDITION                                     │
│  Data race:  no HB between concurrent accesses (JMM concept)    │
│  Race cond:  correctness depends on timing (logic concept)       │
│  They are independent — can have one without the other           │
│                                                                  │
│  FIXES                                                           │
│  synchronized:     atomicity + visibility, may contend           │
│  AtomicInteger:    CAS-based, lock-free, single variable         │
│  ReentrantLock:    tryLock, timeout, conditions                  │
│  ConcurrentMap:    atomic compound operations                    │
│  Immutability:     no mutation → no races                        │
│  Thread confinement: no sharing → no races                       │
│                                                                  │
│  counter++ IS NOT ATOMIC                                         │
│  getfield → iconst_1 → iadd → putfield                          │
│  volatile doesn't help — need AtomicInteger or synchronized      │
└──────────────────────────────────────────────────────────────────┘
```
