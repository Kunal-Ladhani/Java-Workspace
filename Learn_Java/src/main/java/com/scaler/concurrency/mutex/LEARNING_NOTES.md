# Mutex — Complete Guide for SDE2

## Table of Contents

1. [What is a Mutex?](#1-what-is-a-mutex)
2. [Mutex is a Concept, Not a Java Class](#2-mutex-is-a-concept-not-a-java-class)
3. [Implementing Mutex in Java](#3-implementing-mutex-in-java)
4. [Mutex vs Binary Semaphore — THE Critical Difference](#4-mutex-vs-binary-semaphore--the-critical-difference)
5. [Mutex vs Lock vs Monitor — Terminology Clarification](#5-mutex-vs-lock-vs-monitor--terminology-clarification)
6. [Priority Inversion and Priority Inheritance](#6-priority-inversion-and-priority-inheritance)
7. [Reentrant Mutex vs Non-Reentrant Mutex](#7-reentrant-mutex-vs-non-reentrant-mutex)
8. [Mutex in Different Languages](#8-mutex-in-different-languages)
9. [When to Use What in Java](#9-when-to-use-what-in-java)
10. [Interview Questions & Answers](#10-interview-questions--answers)

---

## 1. What is a Mutex?

**Mutex** stands for **Mut**ual **Ex**clusion. It is a synchronization primitive that ensures
only **one thread** can access a critical section at a time.

The defining property of a mutex is **ownership**:

```
Thread A: lock(mutex)
           ↓
       [ critical section — only Thread A can be here ]
           ↓
Thread A: unlock(mutex)    ← ONLY Thread A can unlock!
```

**Key properties of a mutex**:
1. **Mutual Exclusion**: At most one thread holds the mutex at any time.
2. **Ownership**: Only the thread that locked the mutex can unlock it.
3. **No busy-waiting** (typically): Waiting threads are put to sleep, not spinning.
4. **Optional reentrancy**: The same thread may lock the mutex multiple times without
   deadlocking (if the mutex is reentrant).

A mutex is the most fundamental synchronization primitive. Every concurrent system needs
some form of mutual exclusion, and the mutex is the canonical solution.

---

## 2. Mutex is a Concept, Not a Java Class

Unlike `Semaphore` or `ReentrantLock`, there is **no `Mutex` class** in Java's standard
library. Mutex is an **abstract concept** that can be implemented several ways.

This is a common source of confusion. When someone says "use a mutex in Java," they mean:

```
Mutex (concept)
   │
   ├── synchronized keyword     (built into the language)
   ├── ReentrantLock             (java.util.concurrent.locks)
   ├── Semaphore(1)              (approximation — NOT a true mutex)
   └── StampedLock (writeLock)   (java.util.concurrent.locks)
```

Each of these provides mutual exclusion, but with different trade-offs and capabilities.

### Why Java Doesn't Have a Mutex Class

Java's designers decided that `synchronized` (built into every object as a monitor) was
sufficient for basic mutual exclusion. When more features were needed, they added the `Lock`
interface and `ReentrantLock` in Java 5. A separate `Mutex` class would have been redundant.

Some concurrency libraries DO have a `Mutex` class (e.g., Guava's `Monitor`, .NET's `Mutex`),
but Java's standard library doesn't.

---

## 3. Implementing Mutex in Java

### Approach 1: synchronized Keyword

The simplest and most common way to achieve mutual exclusion in Java.

```java
public class Counter {
    private int count = 0;

    public synchronized void increment() {
        count++;  // only one thread at a time
    }

    public synchronized int getCount() {
        return count;
    }
}
```

Or with a synchronized block for finer-grained control:

```java
public class Counter {
    private int count = 0;
    private final Object lock = new Object();

    public void increment() {
        synchronized (lock) {
            count++;
        }
    }
}
```

**Properties as a mutex**:
- ✅ Ownership: only the thread that entered the synchronized block can exit it
- ✅ Reentrant: the same thread can enter a synchronized block it already holds
- ✅ Automatic release: the lock is released when the block exits (even on exception)
- ❌ No tryLock / timeout
- ❌ No fairness control
- ❌ No interruptible locking

### Approach 2: ReentrantLock

The most feature-complete mutex implementation in Java.

```java
import java.util.concurrent.locks.ReentrantLock;

public class Counter {
    private int count = 0;
    private final ReentrantLock lock = new ReentrantLock();

    public void increment() {
        lock.lock();
        try {
            count++;
        } finally {
            lock.unlock();
        }
    }
}
```

**Properties as a mutex**:
- ✅ Ownership: only the thread that called lock() can call unlock()
- ✅ Reentrant: same thread can lock multiple times (hold count tracks depth)
- ✅ tryLock() with optional timeout
- ✅ Fairness option: `new ReentrantLock(true)`
- ✅ Interruptible: `lockInterruptibly()`
- ✅ Condition variables: `lock.newCondition()`
- ❌ Manual release required (must use try-finally)

### Approach 3: Semaphore(1) — NOT a True Mutex

```java
import java.util.concurrent.Semaphore;

Semaphore mutex = new Semaphore(1);

mutex.acquire();
try {
    // critical section
} finally {
    mutex.release();
}
```

**Properties as a mutex**:
- ✅ Only one thread in critical section at a time
- ❌ NO ownership: any thread can call release()
- ❌ NOT reentrant: same thread calling acquire() twice will deadlock
- ❌ No error detection for wrong-thread release
- ❌ No hold-count tracking

**Conclusion**: `Semaphore(1)` provides mutual exclusion but is NOT a true mutex because
it lacks ownership. This distinction is interview-critical.

---

## 4. Mutex vs Binary Semaphore — THE Critical Difference

This is one of the most frequently asked interview questions, and getting it wrong signals
a shallow understanding of concurrency.

### The Core Difference: Ownership

```
MUTEX (e.g., ReentrantLock):
  Thread A: lock()      ← Thread A is the OWNER
  Thread B: unlock()    ← IllegalMonitorStateException! Only A can unlock.

BINARY SEMAPHORE (Semaphore(1)):
  Thread A: acquire()   ← Thread A has the permit, but there's no "owner"
  Thread B: release()   ← WORKS! No error. Permit count goes to 1.
```

### Why Ownership Matters

#### 1. Error Detection

With a mutex, accidentally releasing from the wrong thread is caught as a bug:

```java
// ReentrantLock
lock.lock(); // Thread A locks
// Thread B calls lock.unlock() → throws IllegalMonitorStateException
// Bug detected immediately!

// Semaphore(1)
sem.acquire(); // Thread A acquires
// Thread B calls sem.release() → succeeds silently
// Invariant broken! Two threads could now be in the critical section!
```

#### 2. Reentrancy

```java
// ReentrantLock — reentrant
lock.lock();
lock.lock();    // hold count = 2, no deadlock
lock.unlock();
lock.unlock();  // hold count = 0, fully released

// Semaphore(1) — NOT reentrant
sem.acquire();
sem.acquire();  // DEADLOCK! Thread waits for itself to release.
```

#### 3. Priority Inheritance

When a high-priority thread is waiting for a mutex held by a low-priority thread, the OS
can temporarily boost the low-priority thread's priority (Priority Inheritance Protocol).
This requires knowing WHO owns the mutex. With a semaphore, there's no owner to boost.

#### 4. Debugging

When a mutex is held, debugging tools can show WHICH thread holds it and its stack trace.
With a semaphore, you only know permits = 0 but not who has the permit.

### Summary Table

| Aspect                    | Mutex (ReentrantLock)       | Binary Semaphore (Semaphore(1)) |
|---------------------------|-----------------------------|---------------------------------|
| Ownership                 | Yes — thread-bound          | No — any thread can release     |
| Reentrancy                | Yes (same thread relocks)   | No (same thread deadlocks)      |
| Wrong-thread unlock       | Exception thrown             | Silently succeeds (bug!)        |
| Priority inheritance      | Possible                    | Not possible                    |
| Condition variables       | Yes (newCondition())        | No                              |
| Thread signaling          | Not designed for this       | Yes (release without acquire)   |
| Hold count                | Tracked                     | Not tracked                     |
| Debugging                 | Owner thread visible        | No owner info                   |

---

## 5. Mutex vs Lock vs Monitor — Terminology Clarification

These terms are often used interchangeably, but they mean different things.

### Mutex

- **Abstract concept**: ensures only one thread in a critical section.
- Defines two operations: lock and unlock.
- Has ownership semantics.
- Can be implemented many ways.

### Lock (Java `Lock` Interface)

- **Java-specific interface** in `java.util.concurrent.locks`.
- Defines the API: `lock()`, `unlock()`, `tryLock()`, `lockInterruptibly()`, `newCondition()`.
- Implementations: `ReentrantLock`, `ReentrantReadWriteLock.ReadLock`,
  `ReentrantReadWriteLock.WriteLock`, `StampedLock`.
- A Lock IS a mutex (exclusive mode) plus extra features.

```java
public interface Lock {
    void lock();
    void lockInterruptibly() throws InterruptedException;
    boolean tryLock();
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;
    void unlock();
    Condition newCondition();
}
```

### Monitor

- A **monitor** = mutex + condition variable(s).
- Java's `synchronized` keyword implements the monitor pattern:
  - The intrinsic lock = the mutex part
  - `wait()` / `notify()` / `notifyAll()` = the condition variable part
- Every Java object is a monitor (it has an intrinsic lock and a wait set).

```
Monitor = Mutex + Condition Variable

Java's synchronized:
  ┌──────────────────────────┐
  │  Intrinsic Lock (Mutex)  │  ← synchronized(obj) { ... }
  │                          │
  │  Wait Set (Condition)    │  ← obj.wait(), obj.notify()
  └──────────────────────────┘

ReentrantLock equivalent:
  ┌──────────────────────────┐
  │  ReentrantLock (Mutex)   │  ← lock.lock(), lock.unlock()
  │                          │
  │  Condition (multiple!)   │  ← condition.await(), condition.signal()
  └──────────────────────────┘
```

### Key Insight

- `synchronized` gives you **one** condition per monitor (the object's wait set).
- `ReentrantLock` gives you **multiple** conditions via `newCondition()`.
- This is why `ReentrantLock` + `Condition` is more powerful than `synchronized` for
  complex producer-consumer scenarios.

### Comparison

| Term      | What it is                     | Java Implementation                      |
|-----------|--------------------------------|------------------------------------------|
| Mutex     | Concept — mutual exclusion     | `synchronized`, `ReentrantLock`          |
| Lock      | Interface — mutex + features   | `ReentrantLock`, `ReadWriteLock`         |
| Monitor   | Concept — mutex + condition    | `synchronized` + `wait()/notify()`       |
| Semaphore | Permit-based access control    | `java.util.concurrent.Semaphore`         |

---

## 6. Priority Inversion and Priority Inheritance

### What is Priority Inversion?

Priority inversion occurs when a **high-priority** thread is blocked waiting for a resource
held by a **low-priority** thread, while a **medium-priority** thread preempts the low-priority
thread — effectively inverting the priority order.

```
Time →  ─────────────────────────────────────────────

HIGH:   |---wait for mutex---------|---run---|
                                    ↑
MEDIUM: |---------|---RUNNING------|
                   ↑ preempts LOW
LOW:    |---locked---| (preempted, can't release) ...released|

Problem: HIGH is blocked because MEDIUM (lower priority than HIGH!)
         is preventing LOW from finishing and releasing the mutex.
```

### Real-World Example: Mars Pathfinder (1997)

NASA's Mars Pathfinder suffered priority inversion in 1997. A high-priority bus management
task was blocked by a low-priority meteorological task holding a shared mutex, while
medium-priority communication tasks preempted the low-priority task. The system would
reset itself repeatedly until engineers remotely enabled priority inheritance.

### Priority Inheritance Protocol

The solution: when a high-priority thread blocks on a mutex, the OS temporarily **raises
the priority** of the mutex owner to the priority of the blocked thread. This prevents
medium-priority threads from preempting the owner.

```
With Priority Inheritance:

HIGH:   |---wait for mutex-----|---run---|
                                ↑
MEDIUM: |---------|  (can't preempt — LOW's priority was boosted)
                   
LOW:    |---locked (priority boosted!)---| released
```

### Why This Requires Ownership

Priority inheritance is **only possible with mutexes**, not semaphores. The OS needs to
know WHICH thread holds the resource to boost its priority. With a semaphore, there's
no owner — the OS doesn't know whose priority to boost.

### Java and Priority Inversion

Java's `synchronized` and `ReentrantLock` rely on the JVM's thread scheduling, which in
turn depends on the OS. Most modern OSes (Linux, macOS, Windows) support some form of
priority inheritance for mutexes, but the JVM doesn't expose this directly. In practice,
Java's thread priority (`setPriority()`) has limited effect on most systems, so priority
inversion is more of a theoretical concern in Java but very real in embedded/real-time
systems.

---

## 7. Reentrant Mutex vs Non-Reentrant Mutex

### Reentrant (Recursive) Mutex

A reentrant mutex allows the **same thread** to acquire it multiple times without deadlocking.
It maintains a **hold count** that tracks the nesting depth.

```java
ReentrantLock lock = new ReentrantLock();

lock.lock();    // hold count = 1
lock.lock();    // hold count = 2 (same thread — OK)
lock.lock();    // hold count = 3

lock.unlock();  // hold count = 2
lock.unlock();  // hold count = 1
lock.unlock();  // hold count = 0 — lock is free
```

**Java's `synchronized` is also reentrant**:

```java
public synchronized void methodA() {
    methodB(); // can call another synchronized method on the same object
}

public synchronized void methodB() {
    // same thread already holds the lock — no deadlock
}
```

### Non-Reentrant Mutex

A non-reentrant mutex **deadlocks** if the same thread tries to acquire it twice.

```java
// Conceptual non-reentrant mutex (Java doesn't have one built-in)
nonReentrantMutex.lock();
nonReentrantMutex.lock(); // DEADLOCK — thread waits for itself
```

Java doesn't have a built-in non-reentrant mutex. `Semaphore(1)` behaves like one:

```java
Semaphore sem = new Semaphore(1);
sem.acquire();
sem.acquire(); // DEADLOCK — semaphore doesn't know it's the same thread
```

### When Is Reentrancy Useful?

1. **Recursive algorithms** that need synchronization:
   ```java
   public synchronized int fibonacci(int n) {
       if (n <= 1) return n;
       return fibonacci(n - 1) + fibonacci(n - 2); // recursive + synchronized
   }
   ```

2. **Method composition** where synchronized methods call other synchronized methods:
   ```java
   public synchronized void transferAll(Account target) {
       for (Item item : items) {
           transfer(target, item); // also synchronized — needs reentrancy
       }
   }
   ```

3. **Callback patterns** where a callback might re-enter the locked context.

### When Is Non-Reentrancy Preferred?

Non-reentrant mutexes catch bugs earlier. If your code accidentally tries to acquire the
same lock twice, a non-reentrant mutex deadlocks immediately (easier to detect than subtle
correctness bugs from unexpected reentrancy). Some argue this is safer for defensive
programming.

---

## 8. Mutex in Different Languages

| Language   | Mutex Implementation                        | Notes                              |
|------------|---------------------------------------------|------------------------------------|
| **Java**   | `synchronized`, `ReentrantLock`             | Reentrant, monitor-based           |
| **C++**    | `std::mutex`, `std::recursive_mutex`        | RAII with `std::lock_guard`        |
| **Go**     | `sync.Mutex`, `sync.RWMutex`               | Non-reentrant by design            |
| **Python** | `threading.Lock`, `threading.RLock`         | GIL limits true parallelism        |
| **Rust**   | `std::sync::Mutex<T>`                       | Ownership enforced at compile time |
| **C#/.NET**| `lock` keyword, `Mutex` class               | `Mutex` works across processes     |

### Notable Design Choices

**Go**: Deliberately non-reentrant. Go's philosophy is that if you need a reentrant mutex,
your code structure is wrong. Refactor to avoid nested locking instead.

**Rust**: The `Mutex<T>` wraps the data it protects. You CANNOT access the data without
holding the lock. This is enforced at compile time — impossible to forget to lock.

```rust
let m = Mutex::new(5);
let mut num = m.lock().unwrap(); // must lock to get access
*num = 6;                        // lock auto-released when `num` goes out of scope
```

**C++**: Uses RAII (Resource Acquisition Is Initialization) via `std::lock_guard`:

```cpp
std::mutex mtx;
{
    std::lock_guard<std::mutex> guard(mtx); // lock acquired
    // critical section
} // lock automatically released when guard goes out of scope
```

---

## 9. When to Use What in Java

### Decision Tree for Mutual Exclusion

```
Need mutual exclusion in Java?
│
├── Simple, no special requirements?
│   └── Use synchronized
│       - Simplest, least error-prone
│       - JVM optimizes heavily (biased locking, thin locks)
│       - Automatic release on exception
│
├── Need tryLock, timeout, or interruptibility?
│   └── Use ReentrantLock
│       - Most flexible mutex in Java
│       - Must use try-finally (error-prone if forgotten)
│
├── Need multiple condition variables?
│   └── Use ReentrantLock + Condition
│       - synchronized only has one wait set per object
│       - ReentrantLock can create multiple Conditions
│
├── Need fairness (FIFO ordering)?
│   └── Use ReentrantLock(true)
│       - synchronized has no fairness option
│
├── Need read-write separation?
│   └── Use ReentrantReadWriteLock
│       - Multiple concurrent readers, exclusive writer
│
├── Need to limit concurrent access to N?
│   └── Use Semaphore(N) — NOT a mutex
│       - This is resource pooling, not mutual exclusion
│
└── Need cross-thread signaling?
    └── Use Semaphore(0) as a signal
        - Or CountDownLatch / CyclicBarrier for specific patterns
```

### Quick Reference

| Scenario                              | Tool                        |
|---------------------------------------|-----------------------------|
| Simple critical section               | `synchronized`              |
| Need tryLock or timeout               | `ReentrantLock`             |
| Need fair ordering                    | `ReentrantLock(true)`       |
| Multiple conditions (producer-consumer)| `ReentrantLock` + `Condition` |
| High read-to-write ratio              | `ReentrantReadWriteLock`    |
| Limit concurrent access to N          | `Semaphore(N)`              |
| One-time gate                         | `CountDownLatch`            |
| Cyclic synchronization point          | `CyclicBarrier`             |

---

## 10. Interview Questions & Answers

### Q1: What is a mutex? Does Java have a Mutex class?

**Answer**: A mutex (Mutual Exclusion) is a synchronization concept that ensures only one
thread can execute a critical section at a time. Its key property is **ownership** — only the
locking thread can unlock. Java does NOT have a `Mutex` class. Instead, Java provides:
`synchronized` (built-in monitor), `ReentrantLock` (explicit lock), and `Semaphore(1)`
(approximation, NOT a true mutex because it lacks ownership). For mutual exclusion, prefer
`synchronized` for simplicity or `ReentrantLock` for advanced features.

---

### Q2: What is the difference between a mutex and a binary semaphore?

**Answer**: The **critical** difference is ownership:
- **Mutex**: has an owner thread. Only the owner can unlock it. Supports reentrancy (same
  thread can lock multiple times). Enables priority inheritance. Detects wrong-thread unlock.
- **Binary Semaphore**: has no owner. ANY thread can release. NOT reentrant (same thread
  acquiring twice causes deadlock). Cannot support priority inheritance.

In Java: `ReentrantLock` = mutex, `Semaphore(1)` = binary semaphore. Using `Semaphore(1)`
for mutual exclusion is fragile — if any thread calls `release()` without acquiring, the
mutual exclusion guarantee is silently broken.

---

### Q3: Explain the difference between synchronized and ReentrantLock.

**Answer**:

| Feature                 | synchronized           | ReentrantLock               |
|-------------------------|------------------------|-----------------------------|
| Syntax                  | Keyword (block/method) | API (lock/unlock)           |
| Release                 | Automatic (block exit) | Manual (must use finally)   |
| Reentrancy              | Yes                    | Yes                         |
| Fairness                | No control             | Yes (`new ReentrantLock(true)`) |
| tryLock / timeout       | No                     | Yes                         |
| Interruptible           | No                     | Yes (`lockInterruptibly()`) |
| Condition variables     | 1 per object           | Multiple (`newCondition()`) |
| Performance             | JVM-optimized          | Comparable                  |

Use `synchronized` for simple cases. Use `ReentrantLock` when you need timeout, fairness,
interruptibility, or multiple condition variables.

---

### Q4: What is a monitor? How does Java implement it?

**Answer**: A **monitor** is a synchronization construct that combines a mutex with one or
more condition variables. It provides mutual exclusion and the ability for threads to wait
for conditions. In Java, every object is a monitor:
- **Mutex part**: the intrinsic lock, entered via `synchronized`.
- **Condition part**: `wait()`, `notify()`, `notifyAll()` (inherited from `Object`).

`ReentrantLock` + `Condition` is the explicit equivalent: the lock provides mutual exclusion,
and each `Condition` object provides `await()` / `signal()` / `signalAll()`. The advantage
of `ReentrantLock` is supporting **multiple** condition variables (e.g., "not full" and
"not empty" in a bounded buffer).

---

### Q5: What is priority inversion? How is it related to mutexes?

**Answer**: Priority inversion occurs when a high-priority thread is blocked waiting for a
mutex held by a low-priority thread, while medium-priority threads preempt the low-priority
thread — effectively making the high-priority thread wait behind medium-priority threads.

The solution is **Priority Inheritance**: temporarily boost the mutex owner's priority to
that of the highest-priority waiting thread. This requires **ownership** — the system must
know who holds the mutex. This is why mutexes (with ownership) are preferred over binary
semaphores (without ownership) in priority-sensitive systems. The Mars Pathfinder (1997) is
the famous real-world example of priority inversion causing system failures.

---

### Q6: Why is ReentrantLock reentrant? When is reentrancy useful?

**Answer**: `ReentrantLock` is reentrant because it tracks the **owner thread** and a
**hold count**. When the owning thread calls `lock()` again, instead of blocking, the hold
count increments. Each `unlock()` decrements it. The lock is fully released when hold
count reaches 0.

Reentrancy is useful when: (1) synchronized methods call other synchronized methods on the
same object — without reentrancy, this would deadlock. (2) Recursive algorithms that need
synchronization. (3) Template Method pattern where a base class method acquires a lock
and calls an abstract method that also needs the lock.

---

### Q7: Can you implement a non-reentrant mutex in Java?

**Answer**: Java doesn't provide one out of the box, but you can:

1. **Semaphore(1)**: acts as a non-reentrant mutex since it has no ownership tracking.
   Same thread acquiring twice will deadlock.

2. **Custom implementation** using `AtomicReference<Thread>`:
   ```java
   public class NonReentrantMutex {
       private final AtomicReference<Thread> owner = new AtomicReference<>();
       
       public void lock() {
           Thread current = Thread.currentThread();
           if (owner.get() == current) {
               throw new IllegalStateException("Non-reentrant: already held!");
           }
           while (!owner.compareAndSet(null, current)) {
               LockSupport.park();
           }
       }
       
       public void unlock() {
           if (owner.get() != Thread.currentThread()) {
               throw new IllegalMonitorStateException("Not the owner!");
           }
           owner.set(null);
       }
   }
   ```

Note: Go's `sync.Mutex` is deliberately non-reentrant by design philosophy.

---

### Q8: In a code review, you see Semaphore(1) being used for mutual exclusion. What feedback would you give?

**Answer**: I would flag this as a potential issue and suggest using `ReentrantLock` or
`synchronized` instead. My concerns:

1. **No ownership protection**: Any thread can call `release()`, silently breaking mutual
   exclusion guarantees. With `ReentrantLock`, wrong-thread unlock throws an exception.
2. **No reentrancy**: If the code evolves to have nested locking (method A calls method B,
   both need the lock), `Semaphore(1)` will deadlock while `ReentrantLock` handles it.
3. **No condition variables**: If the code later needs `wait/notify` semantics, `Semaphore`
   can't provide it.
4. **Misleading intent**: `Semaphore(1)` signals "resource pooling" to readers. If the
   intent is mutual exclusion, use a mutex to communicate intent clearly.

The only case where `Semaphore(1)` is appropriate is when you WANT cross-thread signaling
(one thread acquires, a different thread releases) — but then you're not doing mutual
exclusion.
