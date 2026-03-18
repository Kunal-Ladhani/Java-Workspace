# wait(), notify(), join(), sleep() — Complete Guide for SDE2

## Table of Contents

1. [Thread.sleep(long millis)](#1-threadsleeplong-millis)
2. [Thread.yield()](#2-threadyield)
3. [Thread.join()](#3-threadjoin)
4. [Object.wait()](#4-objectwait)
5. [Object.notify()](#5-objectnotify)
6. [Object.notifyAll()](#6-objectnotifyall)
7. [Spurious Wakeups](#7-spurious-wakeups)
8. [The Critical Differences Table](#8-the-critical-differences-table)
9. [wait/notify Pattern — Producer-Consumer](#9-waitnotify-pattern--producer-consumer)
10. [Common Pitfalls](#10-common-pitfalls)
11. [SDE2 Interview Questions & Answers](#11-sde2-interview-questions--answers)

---

## 1. Thread.sleep(long millis)

`Thread.sleep()` pauses the **current thread** for the specified number of milliseconds. It is
a **static** method on the `Thread` class — it always acts on the thread that calls it.

```java
Thread.sleep(2000); // pause current thread for 2 seconds
```

### Key Characteristics

1. **Does NOT release any locks** — this is the single most important fact for interviews.
   If the current thread holds a monitor lock (is inside a `synchronized` block), it keeps
   holding that lock the entire time it is sleeping. Other threads trying to enter the same
   synchronized block will stay BLOCKED until the sleeping thread wakes up and exits.

2. **Thread state → TIMED_WAITING** — while sleeping, `Thread.getState()` returns
   `Thread.State.TIMED_WAITING`.

3. **Throws InterruptedException** — if another thread calls `interrupt()` on the sleeping
   thread, `sleep()` throws `InterruptedException` and the thread's interrupted status is
   cleared.

4. **Accuracy** — sleep is not a real-time guarantee. The actual pause depends on the
   OS scheduler, system load, and timer granularity. It guarantees *at least* the specified
   duration, but may sleep slightly longer.

### sleep(0) — Special Case

`Thread.sleep(0)` is functionally similar to `Thread.yield()`. It tells the scheduler:
"I'm willing to give up my remaining time slice." The scheduler may then pick another
runnable thread, or it may immediately resume the same thread.

### TimeUnit.SECONDS.sleep() — More Readable API

```java
Thread.sleep(5000);          // less readable
TimeUnit.SECONDS.sleep(5);   // same thing, more readable
TimeUnit.MINUTES.sleep(1);   // also available
```

`TimeUnit.sleep()` internally calls `Thread.sleep()` with the converted milliseconds.

### Common Pitfall: Using sleep() for Synchronization

```java
while (!dataReady) {
    Thread.sleep(100); // "busy-wait" with sleep — wasteful and fragile
}
```

This is **polling** — you're guessing at the right sleep duration. Too short → wastes CPU.
Too long → adds latency. No happens-before guarantee. Use `wait()/notify()` or higher-level
constructs (`CountDownLatch`, `CompletableFuture`) instead.

### sleep() Inside a Synchronized Block — Proof

```java
synchronized (lock) {
    Thread.sleep(3000); // lock is NOT released during sleep
}
// Another thread trying to enter synchronized(lock) is BLOCKED for all 3 seconds
```

See `SleepVsWaitDemo.java` for a runnable proof with timestamps.

---

## 2. Thread.yield()

```java
Thread.yield();
```

`yield()` is a **static** method that provides a **hint** to the thread scheduler that the
current thread is willing to give up its current use of the CPU.

### Key Characteristics

1. **It's just a hint** — the scheduler is free to ignore it entirely.
2. **Thread stays in RUNNABLE state** — unlike `sleep()`, it does not move to TIMED_WAITING.
3. **No guarantee of fairness** — no guarantee that any other thread will be selected next.
4. **Rarely used in practice** — use higher-level concurrency utilities instead.
   Sometimes used in spin-locks as a performance optimization, but even that is debatable.

`yield()` vs `sleep(0)`: both are hints to the scheduler. The practical difference is JVM
and OS dependent. For interview purposes, treat them as roughly equivalent.

---

## 3. Thread.join()

`join()` allows one thread to **wait for another thread to complete**. When you call
`t.join()`, the current (calling) thread blocks until thread `t` terminates.

```java
Thread worker = new Thread(() -> {
    // do some work
});
worker.start();
worker.join(); // current thread waits here until worker finishes
System.out.println("Worker is done!");
```

### Overloads

```java
t.join();          // wait indefinitely → current thread state: WAITING
t.join(5000);      // wait at most 5 seconds → state: TIMED_WAITING
t.join(5000, 500); // wait at most 5000ms + 500ns → state: TIMED_WAITING
```

### Key Characteristics

1. **Returns immediately if target is already terminated** — if `t` has already finished
   (or was never started), `t.join()` returns immediately.

2. **Throws InterruptedException** — if the waiting thread is interrupted while blocked
   in `join()`.

3. **Thread state** — the calling thread moves to `WAITING` (for `join()`) or
   `TIMED_WAITING` (for `join(timeout)`).

### Internal Implementation — join() Uses wait()!

This is a favorite interview question. Simplified from the OpenJDK source:

```java
public final synchronized void join(long millis) throws InterruptedException {
    while (isAlive()) {
        wait(millis); // this.wait() — waits on the Thread object itself
    }
}
```

Key observations:
- `join()` is **synchronized** — acquires the monitor on the Thread object.
- Calls `this.wait()` in a loop, checking `isAlive()`.
- When the thread terminates, the JVM calls `notifyAll()` on the Thread object.
- **Never use a Thread object as a lock for your own wait()/notify()** — interferes
  with join().

### Common Pattern: Start N Threads, Join All N

```java
List<Thread> workers = new ArrayList<>();
for (int i = 0; i < 5; i++) {
    Thread t = new Thread(() -> doWork());
    workers.add(t);
    t.start();
}

// Wait for ALL workers to complete
for (Thread t : workers) {
    t.join();
}
System.out.println("All workers done!");
```

This is the simplest form of "fork-join" parallelism. For more complex scenarios, use
`ExecutorService` with `invokeAll()` or `CountDownLatch`.

---

## 4. Object.wait()

`wait()` is defined on `java.lang.Object`, which means **every Java object has it**. It is
the fundamental mechanism for **inter-thread communication** in Java.

### The Contract

`wait()` **MUST** be called inside a `synchronized` block/method on the **same object**.

```java
synchronized (obj) {
    while (!condition) {
        obj.wait(); // MUST be called on the same object used for synchronization
    }
    // condition is now true — proceed
}
```

### What Happens When wait() Is Called — Step by Step

1. **Atomically releases the monitor lock** on the object. This is atomic — there is no
   window where the lock is held but the thread is in the wait-set, or vice versa.

2. **Puts the current thread into the WAITING state** (or TIMED_WAITING for `wait(timeout)`).

3. **Adds the thread to the object's wait-set** — an internal set of threads maintained by
   the JVM for each object's monitor.

4. **Thread is now dormant** — it will not be scheduled until it is notified, interrupted,
   or the timeout expires.

5. **When notified** (via `notify()` or `notifyAll()`):
   - The thread is removed from the wait-set.
   - The thread moves to the **entry-set** (BLOCKED state) and must compete to re-acquire
     the monitor lock.
   - Once the lock is re-acquired, `wait()` returns.

6. **After wait() returns**, the thread holds the monitor lock again and can check the
   condition.

### Overloads

```java
obj.wait();             // wait indefinitely → WAITING
obj.wait(5000);         // wait up to 5 seconds → TIMED_WAITING
obj.wait(5000, 500);    // wait up to 5000ms + 500ns → TIMED_WAITING
```

### Exceptions

- **IllegalMonitorStateException** — if called outside a `synchronized` block on that object.
- **InterruptedException** — if the thread is interrupted while waiting (interrupted status
  is cleared when this exception is thrown).

### Why Must wait() Be Inside synchronized?

To prevent the **lost wakeup problem**. Without `synchronized`, there's a race:

1. Thread A checks: `queue.isEmpty()` → true
2. Thread B adds item and calls `notify()` ← happens between check and wait!
3. Thread A calls `wait()` — signal was already lost, waits forever.

`synchronized` ensures that checking the condition and entering `wait()` is **atomic** with
respect to threads calling `notify()` on the same object.

---

## 5. Object.notify()

`notify()` wakes up **one** thread that is waiting on the object's monitor (i.e., one
thread from the object's wait-set).

```java
synchronized (obj) {
    // change some shared state
    condition = true;
    obj.notify(); // wake up ONE waiting thread
}
```

### Key Characteristics

1. **MUST be called inside synchronized** on the same object, otherwise
   `IllegalMonitorStateException`.

2. **Which thread is woken?** — it is **JVM-dependent**. The specification does not
   guarantee any ordering (not FIFO, not priority-based). The JVM picks one arbitrarily.

3. **Does NOT release the lock** — this is important! `notify()` just moves one thread
   from the wait-set to the entry-set. The notifying thread continues to hold the lock
   until it exits the `synchronized` block. Only then can the woken thread re-acquire
   the lock and return from `wait()`.

4. **The woken thread must re-acquire the lock** — after being notified, the thread
   transitions from WAITING → BLOCKED. It stays BLOCKED until it can re-acquire the
   monitor, at which point it returns from `wait()`.

### Sequence Diagram

```
Thread A (consumer)                   Thread B (producer)
───────────────────                   ───────────────────
synchronized(obj) {
  while (!ready) {
    obj.wait();         ──→ releases lock, enters wait-set
  }                                   synchronized(obj) {    ← acquires lock
                                        ready = true;
                                        obj.notify();        ← wakes Thread A
                                      }                      ← releases lock
  ←── re-acquires lock
  // ready is true, proceed
}
```

---

## 6. Object.notifyAll()

`notifyAll()` wakes up **ALL** threads in the object's wait-set.

```java
synchronized (obj) {
    condition = true;
    obj.notifyAll(); // wake up ALL waiting threads
}
```

### notify() vs notifyAll() — When to Use Which

| Scenario | Use |
|---|---|
| All waiters check the **same** condition and are interchangeable | `notify()` is sufficient |
| Waiters check **different** conditions | **Must** use `notifyAll()` |
| You're not sure | Use `notifyAll()` — it's always safe |

### Why notifyAll() Is Safer — The Missed Signal Problem

Consider a producer-consumer with both "buffer full" and "buffer empty" conditions on the
same lock:

```java
// Producer waits when buffer is full
synchronized (buffer) {
    while (buffer.isFull()) buffer.wait();
    buffer.add(item);
    buffer.notify(); // PROBLEM: might wake another producer instead of a consumer!
}
```

If `notify()` happens to wake another producer (which also finds the buffer full and goes
back to waiting), the signal is effectively lost. A consumer that should have been woken
never gets the signal.

With `notifyAll()`, **all** threads wake up, check their conditions, and the right ones
proceed. The "wrong" ones go back to waiting.

### Performance Consideration

`notifyAll()` causes a "thundering herd" — all threads wake up, but only one (or a few)
can actually proceed. For high-performance scenarios, use `ReentrantLock` with separate
`Condition` objects (`notFull`, `notEmpty`) to signal producers and consumers independently.

---

## 7. Spurious Wakeups

A **spurious wakeup** occurs when a thread returns from `wait()` even though no thread
called `notify()` or `notifyAll()` on the object. This is a real phenomenon caused by:

- **OS-level behavior** — POSIX `pthread_cond_wait()` is explicitly allowed to return
  spuriously (for implementation efficiency).
- **JVM implementation** — the JVM is built on top of OS primitives that have this
  behavior.

### The Rule: ALWAYS Use a While Loop

```java
// ✅ CORRECT — protected against spurious wakeups
synchronized (obj) {
    while (!condition) {
        obj.wait();
    }
    // condition is guaranteed to be true here
}

// ❌ WRONG — vulnerable to spurious wakeups
synchronized (obj) {
    if (!condition) {
        obj.wait();
    }
    // condition might still be false! (spurious wakeup)
}
```

With a `while` loop, even if the thread wakes up spuriously, it re-checks the condition
and goes back to waiting if necessary. With an `if`, the thread proceeds with a false
condition, leading to bugs that are extremely difficult to reproduce and diagnose.

### Why This Is So Important

- Spurious wakeups are **rare** but **real** — bugs are nearly impossible to reproduce.
- Using `while` instead of `if` costs nothing but prevents a catastrophic class of bugs.
- Every official Java doc and "Java Concurrency in Practice" emphasizes this pattern.

---

## 8. The Critical Differences Table

This is the most commonly asked comparison in SDE2 interviews. Know this cold.

| Feature | `sleep()` | `wait()` | `join()` | `yield()` |
|---|---|---|---|---|
| **Defined in** | `Thread` (static) | `Object` (instance) | `Thread` (instance) | `Thread` (static) |
| **Releases lock?** | **NO** | **YES** | N/A (not lock-related) | **NO** |
| **Must be in synchronized?** | No | **YES** (mandatory) | No | No |
| **Thread state** | `TIMED_WAITING` | `WAITING` or `TIMED_WAITING` | `WAITING` or `TIMED_WAITING` | `RUNNABLE` |
| **Woken by** | Timeout or `interrupt()` | `notify()`, `notifyAll()`, or `interrupt()` | Target thread finishes | Scheduler decision |
| **Purpose** | Pause execution for a duration | Inter-thread communication | Wait for another thread to finish | Hint to scheduler |
| **Throws** | `InterruptedException` | `InterruptedException`, `IllegalMonitorStateException` | `InterruptedException` | Nothing |
| **Can specify timeout?** | Always (mandatory param) | Optional (`wait()` vs `wait(ms)`) | Optional (`join()` vs `join(ms)`) | No |
| **Called on** | Current thread (static) | The lock object | The target thread | Current thread (static) |
| **Lock behavior** | Holds all locks | Releases the lock of the object it's called on | N/A | Holds all locks |

### Memory Visibility

- `wait()` and `notify()`/`notifyAll()` provide a **happens-before** relationship:
  actions before `notify()` in one thread are visible to the thread returning from
  `wait()` on the same object.
- `sleep()` provides **no happens-before** relationship with any other thread.
- `join()` provides a **happens-before** relationship: all actions in thread `t` before it
  terminates are visible to the thread that successfully returns from `t.join()`.

---

## 9. wait/notify Pattern — Producer-Consumer

The producer-consumer pattern is the classic use case for `wait()` and `notify()`. Here is
the canonical implementation.

### Shared Buffer

```java
public class SharedBuffer {
    private final Queue<Integer> queue = new LinkedList<>();
    private final int capacity;

    public SharedBuffer(int capacity) {
        this.capacity = capacity;
    }

    public synchronized void produce(int item) throws InterruptedException {
        while (queue.size() == capacity) {
            wait(); // buffer full — wait for consumer to consume
        }
        queue.add(item);
        System.out.println("Produced: " + item + " | Buffer size: " + queue.size());
        notifyAll(); // wake up consumers (and other producers)
    }

    public synchronized int consume() throws InterruptedException {
        while (queue.isEmpty()) {
            wait(); // buffer empty — wait for producer to produce
        }
        int item = queue.poll();
        System.out.println("Consumed: " + item + " | Buffer size: " + queue.size());
        notifyAll(); // wake up producers (and other consumers)
        return item;
    }
}
```

### Why This Works — Step by Step

1. **Producer** calls `produce()`, acquires the object's monitor lock.
2. Checks if the buffer is full. If yes → calls `wait()`, which **releases the lock** and
   enters the wait-set.
3. **Consumer** can now acquire the lock, remove an item, and call `notifyAll()`.
4. `notifyAll()` wakes up the producer. The producer re-acquires the lock (after the
   consumer exits the synchronized method), re-checks the condition (`while` loop), and
   if the buffer is no longer full, adds the item.
5. The same logic works in reverse for the consumer waiting on an empty buffer.

### Why notifyAll() Instead of notify()?

Both producers and consumers wait on the **same object**. `notify()` might wake another
producer instead of a consumer — the signal is effectively lost. `notifyAll()` ensures all
threads check their condition; the ones whose condition isn't met go back to waiting.

---

## 10. Common Pitfalls

### Pitfall 1: Calling wait()/notify() Outside synchronized

```java
Object obj = new Object();
obj.wait();    // IllegalMonitorStateException!
obj.notify();  // IllegalMonitorStateException!
```

**Fix:** Always call inside `synchronized(obj) { ... }`.

### Pitfall 2: Using if Instead of while with wait()

```java
// BUG: spurious wakeup or condition changed by another thread
synchronized (obj) {
    if (!condition) {
        obj.wait();
    }
    // Might reach here with condition still false!
}
```

**Fix:** Use `while (!condition) { obj.wait(); }`.

### Pitfall 3: Using sleep() for Inter-Thread Coordination

```java
// ANTI-PATTERN: polling with sleep
while (!dataReady) {
    Thread.sleep(100);
}
```

This wastes CPU, adds unnecessary latency, and doesn't provide proper synchronization
guarantees.

**Fix:** Use `wait()/notify()`, `CountDownLatch`, `CompletableFuture`, etc.

### Pitfall 4: Calling notify() Before wait() — Lost Signal

```java
// Thread B (producer) — runs first:
synchronized (obj) {
    ready = true;
    obj.notify();  // nobody is waiting yet — signal is LOST
}

// Thread A (consumer) — runs second:
synchronized (obj) {
    while (!ready) {
        obj.wait(); // waits forever if it doesn't check the condition first
    }
}
```

Wait — actually this code IS correct! Because Thread A checks the `ready` flag in the
`while` condition **before** calling `wait()`. If Thread B already set `ready = true`,
Thread A's `while (!ready)` is false, so it never enters `wait()`.

The **real** lost signal problem happens when you don't use a condition variable (flag):

```java
// Thread B — runs first:
synchronized (obj) {
    obj.notify();  // nobody is waiting → signal is LOST forever
}

// Thread A — runs second:
synchronized (obj) {
    obj.wait();    // waits forever — the notify already happened
}
```

**Fix:** Always pair `wait()` with a condition variable and check it in a `while` loop.
The condition variable acts as a persistent "memory" of the signal, unlike `notify()` which
is fire-and-forget.

### Pitfall 5: Using a Thread Object as a Lock

```java
Thread worker = new Thread(() -> doWork());
worker.start();

synchronized (worker) {
    worker.wait(); // DANGER: interferes with join()'s internal wait/notify
}
```

Since `Thread.join()` internally calls `wait()` on the Thread object, and the JVM calls
`notifyAll()` on the Thread object when it terminates, using a Thread object as your own
lock will cause unpredictable interference.

**Fix:** Always use a dedicated lock object: `private final Object lock = new Object();`

### Pitfall 6: Forgetting That notify() Doesn't Release the Lock

`notify()` signals a thread, but the notifying thread keeps holding the lock until the
`synchronized` block exits. The woken thread stays in BLOCKED state until then.

---

## 11. SDE2 Interview Questions & Answers

### Q1: What is the difference between sleep() and wait()?

**Answer:** See the comparison table in [Section 8](#8-the-critical-differences-table).
The single most critical difference: **sleep() holds locks, wait() releases them.** This
is the #1 most asked follow-up in interviews.

---

### Q2: Why must wait() be called inside a synchronized block?

**Answer:**

To prevent the **lost wakeup problem**. Without synchronization, there's a race condition
between checking the condition and entering `wait()`:

1. Thread A checks: `queue.isEmpty()` → true
2. Thread B adds an item and calls `notify()`
3. Thread A calls `wait()` — but the notify already happened!
4. Thread A waits forever.

By requiring `synchronized`, the JVM ensures that the condition check + `wait()` is
atomic. No other thread can call `notify()` between the check and the wait because it
would need the same lock.

If you try to call `wait()` without holding the monitor, the JVM throws
`IllegalMonitorStateException`.

---

### Q3: Why should wait() always be in a while loop, not an if?

**Answer:**

Two reasons:

1. **Spurious wakeups** — the JVM/OS may wake a thread from `wait()` without any call to
   `notify()` or `notifyAll()`. With `if`, the thread proceeds even though the condition
   is false. With `while`, it re-checks and goes back to waiting.

2. **Condition changed by another thread** — if multiple threads are waiting and
   `notifyAll()` wakes them all, only one might be able to proceed (e.g., only one item
   in the buffer). The others must re-check the condition.

```java
// Always this:
while (!condition) { obj.wait(); }
// Never this:
if (!condition) { obj.wait(); }
```

---

### Q4: What is the difference between notify() and notifyAll()?

**Answer:**

- `notify()` wakes **one** arbitrary thread from the wait-set.
- `notifyAll()` wakes **all** threads from the wait-set.

Use `notify()` when:
- All waiting threads check the same condition.
- Only one thread needs to be woken.
- All waiters are equivalent.

Use `notifyAll()` when:
- Waiting threads check different conditions.
- You use the same lock for multiple types of waiters (e.g., producers and consumers).
- You're unsure — `notifyAll()` is always correct (just less efficient).

The risk with `notify()` is the **missed signal**: it might wake a thread whose condition
isn't met, while the thread that should have been woken stays asleep.

---

### Q5: What happens if notify() is called and no thread is waiting?

**Answer:**

Nothing. The signal is simply **lost**. `notify()` is fire-and-forget — it has no memory.
If no thread is currently in the wait-set, the notification is discarded. A thread that
subsequently calls `wait()` will block as if the `notify()` never happened.

This is why you must always use a **condition variable** (a boolean flag or similar) to
remember the state:

```java
synchronized (lock) {
    while (!ready) {
        lock.wait();
    }
}
```

Even if `notify()` was called before `wait()`, the `ready` flag persists and the `while`
check prevents unnecessary waiting.

---

### Q6: How does join() work internally?

**Answer:**

`Thread.join()` is a `synchronized` method that internally calls `wait()` on the Thread
object itself in a loop:

```java
public final synchronized void join(long millis) throws InterruptedException {
    while (isAlive()) {
        wait(millis); // this.wait() — waits on the Thread object
    }
}
```

When the thread terminates, the JVM calls `notifyAll()` on the Thread object, waking up
all threads blocked in `join()`.

**Key implication:** Never use a Thread object as a lock for your own `wait()/notify()`
because it will interfere with `join()`'s internal mechanism.

---

### Q7: Can a thread wake up from wait() without being notified?

**Answer:**

Yes. This is called a **spurious wakeup**. The Java Language Specification explicitly
allows it (JLS §17.2.1). It happens because the JVM uses OS-level primitives
(`pthread_cond_wait` on POSIX systems) that are allowed to return spuriously for
implementation efficiency.

This is why `wait()` must always be in a `while` loop that re-checks the condition.

---

### Q8: If sleep() doesn't release the lock, why would you ever use it inside synchronized?

**Answer:** Generally, you shouldn't — it blocks other threads unnecessarily. Rare uses
include simulating slow operations in tests. In production, use `wait(timeout)` or
`Condition.await(timeout, unit)` for a timed wait that releases the lock.

---

### Q9: What is the difference between BLOCKED and WAITING thread states?

**Answer:**

| State | Meaning |
|---|---|
| `BLOCKED` | Thread is trying to **acquire** a monitor lock (enter a `synchronized` block) that another thread holds. |
| `WAITING` | Thread has **voluntarily** released the CPU and is waiting for a specific signal (`notify()`, `join()` completion, `LockSupport.unpark()`). |
| `TIMED_WAITING` | Same as `WAITING` but with a timeout — thread will also wake up when the timeout expires. |

A thread goes from `WAITING` → `BLOCKED` when it is notified (it must re-acquire the lock
before `wait()` can return). It goes from `BLOCKED` → `RUNNABLE` when it successfully
acquires the lock.

---

### Q10: Design a simple thread-safe blocking queue using wait/notify.

**Answer:**

```java
public class BlockingQueue<T> {
    private final Queue<T> queue = new LinkedList<>();
    private final int capacity;

    public BlockingQueue(int capacity) {
        this.capacity = capacity;
    }

    public synchronized void put(T item) throws InterruptedException {
        while (queue.size() == capacity) {
            wait();
        }
        queue.add(item);
        notifyAll();
    }

    public synchronized T take() throws InterruptedException {
        while (queue.isEmpty()) {
            wait();
        }
        T item = queue.poll();
        notifyAll();
        return item;
    }

    public synchronized int size() {
        return queue.size();
    }
}
```

Key points: `synchronized` methods (lock is `this`), `while` loop (spurious wakeup
protection), `notifyAll()` (producers and consumers share the same lock), bounded buffer
(`put()` blocks when full, `take()` blocks when empty). In production, use
`ArrayBlockingQueue` or `LinkedBlockingQueue` which have separate locks for put and take.

---

## Quick Reference Cheat Sheet

```java
Thread.sleep(2000);                // pause 2s, does NOT release locks
TimeUnit.SECONDS.sleep(2);         // same thing, more readable
Thread.yield();                    // hint: give up CPU time slice (may be ignored)

Thread t = new Thread(() -> work());
t.start();
t.join();                          // block until t finishes (WAITING)
t.join(5000);                      // block at most 5s (TIMED_WAITING)

// wait/notify — ALWAYS inside synchronized, ALWAYS use while loop
synchronized (lock) {
    while (!condition) lock.wait();    // releases lock, enters wait-set
}
synchronized (lock) {
    condition = true;
    lock.notifyAll();                  // wakes all waiters (does NOT release lock)
}
```
