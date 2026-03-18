# Thread Lifecycle Deep Dive — Complete Guide for SDE2

## Table of Contents

1. [What is a Thread?](#1-what-is-a-thread)
2. [Thread Lifecycle / Thread States](#2-thread-lifecycle--thread-states)
3. [Thread Priority](#3-thread-priority)
4. [Daemon vs User Threads](#4-daemon-vs-user-threads)
5. [Inspecting Thread State: Thread.getState()](#5-inspecting-thread-state-threadgetstate)
6. [Thread Interruption](#6-thread-interruption)
7. [UncaughtExceptionHandler](#7-uncaughtexceptionhandler)
8. [Common Pitfalls](#8-common-pitfalls)
9. [SDE2 Interview Questions & Answers](#9-sde2-interview-questions--answers)

---

## 1. What is a Thread?

A **thread** is the smallest unit of execution that can be scheduled by an operating system.
Every Java program starts with at least one thread — the **main thread** — which executes
the `main()` method.

### OS Thread vs Java Thread

In HotSpot JVM, every `java.lang.Thread` maps **1:1 to a native OS thread** (kernel thread).

```
┌──────────────────────────────────────────────────────┐
│                     JVM Process                      │
│                                                      │
│   Java Thread-0 ──────── OS Thread (pthread/kthread) │
│   Java Thread-1 ──────── OS Thread (pthread/kthread) │
│   Java Thread-2 ──────── OS Thread (pthread/kthread) │
│                                                      │
│   Each Java thread has its own:                      │
│     • JVM stack (method frames)                      │
│     • Program counter                                │
│     • Native stack (for JNI calls)                   │
│                                                      │
│   All threads share:                                 │
│     • Heap memory (objects)                          │
│     • Method area (class metadata)                   │
│     • Runtime constant pool                          │
└──────────────────────────────────────────────────────┘
```

**Implications of 1:1 mapping:**

- Creating a Java thread = creating an OS thread (~1MB stack, kernel data structures).
- Thread scheduling is done by the **OS scheduler**, not the JVM.
- Limits scalability — 10,000+ threads becomes impractical (memory + context switches).

### Project Loom: Virtual Threads (Java 21+)

Java 21 introduced **virtual threads** — lightweight threads managed by the JVM, not the OS.

```
┌──────────────────────────────────────────────────────┐
│  Platform Threads (1:1 with OS)                      │
│    ┌──────────┐   ┌──────────┐   ┌──────────┐        │
│    │ Carrier  │   │ Carrier  │   │ Carrier  │        │
│    │ Thread-0 │   │ Thread-1 │   │ Thread-2 │        │
│    └────┬─────┘   └────┬─────┘   └────┬─────┘        │
│    ┌────┴────┐    ┌────┴────┐    ┌────┴────┐         │
│    │ VT-0    │    │ VT-3    │    │ VT-6    │         │
│    │ VT-1    │    │ VT-4    │    │ VT-7    │         │
│    │ VT-2    │    │ VT-5    │    │ VT-8    │         │
│    └─────────┘    └─────────┘    └─────────┘         │
│                                                      │
│  M:N mapping: millions of virtual threads            │
│  multiplexed onto a small pool of carrier threads.   │
└──────────────────────────────────────────────────────┘
```


| Aspect        | Platform Thread    | Virtual Thread (Loom)          |
| ------------- | ------------------ | ------------------------------ |
| OS mapping    | 1:1 with OS thread | M:N (many VTs on few carriers) |
| Stack size    | ~1MB default       | ~few KB (grows on demand)      |
| Creation cost | Expensive          | Cheap (like an object)         |
| Count limit   | Thousands          | Millions                       |
| Scheduling    | OS scheduler       | JVM scheduler (ForkJoinPool)   |
| Best for      | CPU-bound work     | I/O-bound / blocking work      |


> **Interview note:** Virtual threads do NOT replace platform threads. They complement
> them for I/O-heavy workloads. CPU-bound work still benefits from platform threads.

### Thread vs Process — Key Differences


| Aspect              | Process                           | Thread                                  |
| ------------------- | --------------------------------- | --------------------------------------- |
| Memory space        | Own address space (isolated)      | Shares address space with other threads |
| Communication       | IPC (pipes, sockets, shared mem)  | Direct — shared heap, variables         |
| Creation cost       | Heavy (fork, copy page tables)    | Lighter (shared address space)          |
| Context switch cost | Expensive (TLB flush, page table) | Cheaper (same address space)            |
| Crash isolation     | One crash doesn't kill others     | One thread crash can kill the process   |
| Resource overhead   | Higher (own file descriptors)     | Lower (shared resources)                |


**Why threads for concurrency in Java?** Threads share the same heap (easy data sharing),
have lower memory footprint, and faster context switching. The JVM is a single process;
all Java threads live inside it.

---

## 2. Thread Lifecycle / Thread States

Java defines **exactly 6 thread states** in the `Thread.State` enum:

```java
public enum Thread.State {
    NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED
}
```

### 2.1 NEW

Thread object created but `start()` has **not** been called yet.

```java
Thread t = new Thread(() -> System.out.println("Hello"));
// t.getState() == NEW — no OS thread exists yet
```

### 2.2 RUNNABLE

Thread is **eligible to run**. May be executing on a CPU core or in the OS ready queue.

**Critical nuance:** Java's RUNNABLE combines **two OS-level states**:

```
┌──────────────────────────────────────┐
│          Java: RUNNABLE              │
│   ┌──────────┐     ┌─────────────┐  │
│   │  Ready   │ ←──→│   Running   │  │
│   │ (in OS   │     │ (on CPU     │  │
│   │  queue)  │     │  core)      │  │
│   └──────────┘     └─────────────┘  │
│                                      │
│   Java cannot distinguish between    │
│   these two states.                  │
└──────────────────────────────────────┘
```

A thread doing blocking I/O (`Socket.read()`) also appears as RUNNABLE in Java, even
though the OS considers it blocked.

### 2.3 BLOCKED

Thread is **waiting to acquire a monitor lock** to enter a `synchronized` block/method.

```java
synchronized (sharedObject) {
    // Thread-A holds this lock
    // Thread-B trying to enter → BLOCKED
}
```

**Key:** BLOCKED is **exclusively** for monitor locks (`synchronized`). Waiting for a
`ReentrantLock` puts the thread in WAITING (because it uses `LockSupport.park()`).

### 2.4 WAITING

Thread is waiting **indefinitely** for another thread to perform a specific action.


| Method               | Wakes up when                                 |
| -------------------- | --------------------------------------------- |
| `Object.wait()`      | Another thread calls `notify()`/`notifyAll()` |
| `Thread.join()`      | The target thread terminates                  |
| `LockSupport.park()` | Another thread calls `unpark(thisThread)`     |


### 2.5 TIMED_WAITING

Waiting for another thread OR for a **timeout** — whichever comes first.


| Method                         | Wakes up when                |
| ------------------------------ | ---------------------------- |
| `Thread.sleep(ms)`             | Timeout expires              |
| `Object.wait(ms)`              | notify/notifyAll OR timeout  |
| `Thread.join(ms)`              | Target terminates OR timeout |
| `LockSupport.parkNanos(nanos)` | unpark OR timeout            |


### 2.6 TERMINATED

Thread has **finished execution** — either `run()` completed normally or an uncaught
exception was thrown. **A terminated thread cannot be restarted** — calling `start()`
throws `IllegalThreadStateException`.

---

### Complete State Transition Diagram

```
                            ┌──────────────────────────────────────────────────────────────────────┐
                            │                   THREAD STATE TRANSITIONS                           │
                            └──────────────────────────────────────────────────────────────────────┘

                                                    start()
                                ┌───────┐  ─────────────────────────►  ┌──────────┐
                                │  NEW  │                              │ RUNNABLE │
                                └───────┘                              └──────────┘
                                                                        │  │  │  │
                         ┌──────────────────────────────────────────────┘  │  │  └────────────────────────┐
                         │                          ┌─────────────────────┘  └──────────────┐             │
                         │                          │                                      │             │
                         ▼                          ▼                                      ▼             ▼
              ┌──────────────────┐      ┌───────────────────┐                   ┌──────────────┐   ┌────────────┐
              │     BLOCKED      │      │     WAITING       │                   │TIMED_WAITING │   │ TERMINATED │
              │                  │      │                   │                   │              │   │            │
              │ Trying to enter  │      │ wait()            │                   │ sleep(ms)    │   │ run() done │
              │ synchronized     │      │ join()            │                   │ wait(ms)     │   │ or uncaught│
              │ block/method     │      │ LockSupport.park()│                   │ join(ms)     │   │ exception  │
              └──────────────────┘      └───────────────────┘                   │ parkNanos()  │   └────────────┘
                         │                          │                           └──────────────┘
                         │                          │                                      │
                         │  Lock acquired           │  notify()/notifyAll()                │  Timeout expires
                         │                          │  unpark()                            │  or notify/unpark
                         │                          │  join target completes               │
                         └──────────────┐           │            ┌─────────────────────────┘
                                        ▼           ▼            ▼
                                      ┌──────────────────────────────┐
                                      │          RUNNABLE            │
                                      │  (back to running/ready)     │
                                      └──────────────────────────────┘
```

### Transition Summary Table


| From          | To            | Trigger                                                     |
| ------------- | ------------- | ----------------------------------------------------------- |
| NEW           | RUNNABLE      | `start()` called                                            |
| RUNNABLE      | BLOCKED       | Trying to enter `synchronized` block held by another thread |
| BLOCKED       | RUNNABLE      | Monitor lock acquired                                       |
| RUNNABLE      | WAITING       | `wait()`, `join()`, `LockSupport.park()`                    |
| WAITING       | RUNNABLE      | `notify()`, `notifyAll()`, `unpark()`, join target dies     |
| RUNNABLE      | TIMED_WAITING | `sleep(ms)`, `wait(ms)`, `join(ms)`, `parkNanos()`          |
| TIMED_WAITING | RUNNABLE      | Timeout expires, `notify()`, `unpark()`                     |
| RUNNABLE      | TERMINATED    | `run()` completes normally or uncaught exception            |


### Important Nuances (Interview Gold)

**1. RUNNABLE ≠ Running.** Java does NOT have a "RUNNING" state. The JVM has no way to
distinguish a thread on CPU vs one in the OS ready queue.

**2. BLOCKED is ONLY for monitor locks.** Waiting for `ReentrantLock` → WAITING state
(because `ReentrantLock.lock()` internally uses `LockSupport.park()`).

```
synchronized (obj) { ... }    → Thread waiting here: BLOCKED
reentrantLock.lock();          → Thread waiting here: WAITING
```

**3. WAITING → RUNNABLE requires re-acquiring the monitor.** When `notify()` wakes a
thread from `wait()`, it must first re-acquire the monitor lock. If another thread holds
the lock, the actual path is: `WAITING → BLOCKED → RUNNABLE`.

**4. A TERMINATED thread cannot be restarted.** Calling `start()` again throws
`IllegalThreadStateException`. The OS thread has been destroyed.

---

## 3. Thread Priority

Every Java thread has a **priority** from 1 to 10.

```java
Thread.MIN_PRIORITY  = 1
Thread.NORM_PRIORITY = 5  // default
Thread.MAX_PRIORITY  = 10
```

```java
Thread t = new Thread(() -> doWork());
t.setPriority(Thread.MAX_PRIORITY);
t.start();
```

A new thread inherits the priority of its **parent thread**.

### Priority is a HINT, Not a Guarantee

The thread priority is a **suggestion** to the OS scheduler. The OS may ignore it.

- **Linux:** Mostly ignores Java priority. Uses CFS (Completely Fair Scheduler).
- **Windows:** Respects priority to some extent, mapping to Windows priority levels.
- **macOS:** Limited effect, similar to Linux.

> **Interview answer:** "Thread priority is a hint. NEVER rely on it for correctness.
> If correctness depends on scheduling order, use explicit synchronization."

### Priority Inversion Problem

A **high-priority thread** waits for a lock held by a **low-priority thread**, while a
**medium-priority thread** preempts the low-priority one — effectively making the
high-priority thread wait behind the medium-priority thread.

```
┌─────────────────────────────────────────────────────────────────┐
│ Low (L)  ──────[holds lock]──────────────[preempted by M]────── │
│ Med (M)  ──────────────────[runs]────────[runs]──────────────── │
│ High (H) ──────────[needs lock → BLOCKED]──────────────[waits]  │
│                                                                 │
│ H waits for L to release the lock, but M keeps preempting L.   │
│ Result: H (highest priority) effectively waits the longest!     │
└─────────────────────────────────────────────────────────────────┘
```

**Solution: Priority Inheritance** — the OS temporarily boosts L's priority to match H.
Java's `synchronized` and `ReentrantLock` do NOT implement this — it depends on the OS.

---

## 4. Daemon vs User Threads

- **User threads** (default) — keep the JVM alive.
- **Daemon threads** — background threads; JVM exits when only daemon threads remain.

### Setting a Thread as Daemon

```java
Thread t = new Thread(() -> { /* background work */ });
t.setDaemon(true); // MUST be called BEFORE start()
t.start();
```

Calling `setDaemon()` after `start()` throws `IllegalThreadStateException`.

### JVM Shutdown Rule

```
┌──────────────────────────────────────────────────┐
│  User Thread-1 ──────────── [finishes]           │
│  User Thread-2 ──────────────────── [finishes]   │
│  Daemon Thread-1 ──────────────────── [killed!]  │
│  Daemon Thread-2 ──────────────────── [killed!]  │
│                                                  │
│  ← JVM exits here (all user threads done)        │
│    Daemon threads killed abruptly — NO finally   │
│    blocks, NO cleanup, NO graceful shutdown.      │
└──────────────────────────────────────────────────┘
```

### Key Properties


| Property          | User Thread           | Daemon Thread            |
| ----------------- | --------------------- | ------------------------ |
| Default           | Yes (all new threads) | No (must explicitly set) |
| Keeps JVM alive   | Yes                   | No                       |
| Graceful shutdown | Yes (finally blocks)  | No (abruptly killed)     |
| Child thread type | Inherits parent type  | Inherits parent type     |


### Daemon Thread Use Cases

- **Garbage Collector**, **JIT Compiler**, **Finalizer thread** — all JVM internal daemons
- **Custom:** heartbeat senders, log flushers, cache cleanup, timer threads

> **Interview caution:** Never use daemon threads for critical work (file writes, DB ops)
> because they can be killed mid-operation without cleanup.

---

## 5. Inspecting Thread State: Thread.getState()

`Thread.getState()` returns the current `Thread.State` enum value.

```java
Thread t = new Thread(() -> {
    try { Thread.sleep(5000); }
    catch (InterruptedException e) { Thread.currentThread().interrupt(); }
});

System.out.println(t.getState()); // NEW
t.start();
Thread.sleep(100);
System.out.println(t.getState()); // TIMED_WAITING
t.join();
System.out.println(t.getState()); // TERMINATED
```

### Dumping All Thread States

```java
Map<Thread, StackTraceElement[]> allThreads = Thread.getAllStackTraces();
for (Map.Entry<Thread, StackTraceElement[]> entry : allThreads.entrySet()) {
    Thread thread = entry.getKey();
    System.out.printf("[%s] %s - %s (daemon=%b)%n",
        thread.getId(), thread.getName(), thread.getState(), thread.isDaemon());
}
```

> **Tip:** `jstack <pid>` from the command line does the same — dumps all thread states
> and stack traces. Invaluable for diagnosing deadlocks in production.

---

## 6. Thread Interruption

Thread interruption is Java's **cooperative mechanism** for signaling a thread to stop.
It does NOT forcibly stop the thread — the thread must check and respond.

### The Three Key Methods

```java
thread.interrupt();                // set the interrupt flag (or throw InterruptedException)
thread.isInterrupted();            // check flag WITHOUT clearing
Thread.interrupted();              // check AND clear flag (static — checks CURRENT thread)
```

### How interrupt() Works

**Case 1: Thread is in sleep()/wait()/join()**
→ `InterruptedException` is thrown immediately AND the interrupt flag is **cleared**.

```java
try {
    Thread.sleep(10000);
} catch (InterruptedException e) {
    // flag is CLEARED — Thread.currentThread().isInterrupted() == false
}
```

**Case 2: Thread is running normally**
→ The interrupt flag is set to `true`. Nothing else happens. The thread must check it.

```java
while (!Thread.currentThread().isInterrupted()) {
    doWork();
}
```

**Case 3: Thread is on NIO InterruptibleChannel**
→ `ClosedByInterruptException` is thrown, channel is closed. Classic I/O does NOT respond.

### isInterrupted() vs Thread.interrupted()


| Method                   | Instance/Static | Clears Flag? | Checks Which Thread? |
| ------------------------ | --------------- | ------------ | -------------------- |
| `thread.isInterrupted()` | Instance        | No           | The target thread    |
| `Thread.interrupted()`   | Static          | Yes          | The current thread   |


### Proper Interrupt Handling Patterns

**Pattern 1: Propagate the exception (preferred)**

```java
public void doTask() throws InterruptedException {
    Thread.sleep(1000);
}
```

**Pattern 2: Restore the interrupt flag** (when you can't propagate, e.g., `Runnable.run()`)

```java
@Override
public void run() {
    try {
        Thread.sleep(1000);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt(); // restore the flag!
        return;
    }
}
```

**Pattern 3: Check flag in a loop**

```java
@Override
public void run() {
    while (!Thread.currentThread().isInterrupted()) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
        }
    }
    cleanup();
}
```

### The Cardinal Sin: Swallowing InterruptedException

```java
// NEVER DO THIS ❌
try {
    Thread.sleep(1000);
} catch (InterruptedException e) {
    // Silently swallowed — the interrupt is LOST forever.
    // The flag was cleared when the exception was thrown.
    // Code up the call stack will never know this thread was interrupted.
}
```

> **Interview rule:** If you catch `InterruptedException`, you MUST either re-throw it
> or call `Thread.currentThread().interrupt()` to restore the flag.

---

## 7. UncaughtExceptionHandler

When a thread throws an exception not caught by any `catch` block, the thread terminates
and the JVM invokes its `UncaughtExceptionHandler`.

### Default Behavior

The JVM prints the stack trace to `System.err`. Other threads continue normally.

### Setting Handlers

```java
// Per-thread handler
thread.setUncaughtExceptionHandler((t, ex) -> {
    System.err.println("Thread " + t.getName() + " crashed: " + ex.getMessage());
});

// Global default for ALL threads
Thread.setDefaultUncaughtExceptionHandler((t, ex) -> {
    System.err.println("[GLOBAL] Uncaught in " + t.getName() + ": " + ex.getMessage());
});
```

### Handler Resolution Order

```
1. Per-thread handler    → thread.getUncaughtExceptionHandler()
2. ThreadGroup handler   → threadGroup.uncaughtException()
3. Default handler       → Thread.getDefaultUncaughtExceptionHandler()
4. Fallback              → Print stack trace to System.err
```

### Real-World Use Cases

- **Logging:** Send uncaught exceptions to centralized logging (Sentry, DataDog).
- **Recovery:** Restart a critical worker thread that crashed.
- **Alerting:** Page on-call engineer for critical thread failures.

> **Interview note:** `ExecutorService` behaves differently. Tasks via `execute()` trigger
> the handler on exception. Tasks via `submit()` capture the exception in the `Future` —
> the handler is NOT called; you must call `future.get()` to see the exception.

---

## 8. Common Pitfalls

### Pitfall 1: Calling run() Instead of start()

```java
Thread t = new Thread(() ->
    System.out.println("On: " + Thread.currentThread().getName()));

t.run();   // "On: main"     ← WRONG: runs on caller's thread, no new thread
t.start(); // "On: Thread-0" ← CORRECT: runs on a new OS thread
```

### Pitfall 2: Swallowing InterruptedException

The flag is already cleared when the exception is thrown. If you don't restore it, the
interrupt is permanently lost. Always re-throw or call `Thread.currentThread().interrupt()`.

### Pitfall 3: Relying on Thread Priority

Priority is a hint to the OS scheduler. Behavior varies across platforms. Use locks,
semaphores, or latches for ordering guarantees — never thread priority.

### Pitfall 4: Starting a Thread Twice

```java
Thread t = new Thread(() -> {});
t.start();
t.start(); // IllegalThreadStateException — even after termination!
```

### Pitfall 5: setDaemon() After start()

```java
t.start();
t.setDaemon(true); // IllegalThreadStateException!
```

### Pitfall 6: Assuming Thread.sleep() is Precise

`Thread.sleep(ms)` guarantees a **minimum** sleep. Actual sleep may be longer due to OS
scheduling granularity (~1-15ms) and system load.

### Pitfall 7: Using getState() for Synchronization Logic

`getState()` is a snapshot. The state can change between reading and acting on it.
Use it only for monitoring/debugging — never for correctness.

---

## 9. SDE2 Interview Questions & Answers

### Q1: What are the 6 thread states in Java? Draw the state transition diagram.

**A:** NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED — defined in
`Thread.State` enum. NEW → RUNNABLE via `start()`. RUNNABLE can go to BLOCKED (waiting
for monitor lock), WAITING (`wait()`/`join()`/`park()`), TIMED_WAITING (`sleep()`/
`wait(ms)`/`join(ms)`), or TERMINATED (run() completes). BLOCKED/WAITING/TIMED_WAITING
all return to RUNNABLE when their condition is met.

### Q2: What is the difference between BLOCKED and WAITING?

**A:** BLOCKED = waiting for a **monitor lock** (`synchronized` keyword only). WAITING =
waiting for a specific thread action (`wait()`, `join()`, `park()`). A thread waiting for
`ReentrantLock` is WAITING, not BLOCKED, because ReentrantLock uses `LockSupport.park()`.

### Q3: Can you restart a terminated thread?

**A:** No. `start()` on a terminated thread throws `IllegalThreadStateException`. The
underlying OS thread is destroyed. You must create a new Thread object.

### Q4: What is the difference between run() and start()?

**A:** `start()` creates a new OS thread and invokes `run()` on it. Calling `run()`
directly is just a regular method call — executes on the current thread with no new
thread created.

### Q5: What is the difference between daemon and user threads?

**A:** User threads keep the JVM alive. Daemon threads don't — JVM exits when only
daemon threads remain, killing them abruptly with no finally/cleanup. Set with
`setDaemon(true)` before `start()`. GC is the classic daemon thread example.

### Q6: Explain thread interruption. What happens when you call interrupt()?

**A:** It's cooperative. If the thread is in sleep/wait/join, `InterruptedException`
is thrown and the flag is cleared. If running normally, the flag is set to true and
nothing happens automatically — the thread must check `isInterrupted()`. Never swallow
`InterruptedException` — either propagate or restore the flag.

### Q7: What is the difference between isInterrupted() and Thread.interrupted()?

**A:** `isInterrupted()` is instance method, checks the target thread's flag, does NOT
clear it. `Thread.interrupted()` is static, checks the **current** thread's flag, AND
**clears** it. Second consecutive call returns false (unless re-interrupted).

### Q8: What is priority inversion? How do you handle it?

**A:** A high-priority thread waits for a lock held by a low-priority thread, while a
medium-priority thread preempts the low-priority one. Fix: **priority inheritance** (OS
boosts the low-priority thread temporarily) or **priority ceiling**. Java doesn't
natively implement priority inheritance.

### Q9: What happens when a thread throws an uncaught exception?

**A:** The thread terminates. JVM invokes the `UncaughtExceptionHandler` chain:
per-thread → ThreadGroup → default global → fallback (print to stderr). Other threads
are unaffected. With `ExecutorService.submit()`, exceptions are captured in the Future,
not sent to the handler.

### Q10: Why does Java combine "Ready" and "Running" into RUNNABLE?

**A:** Java's thread state model is a JVM-level abstraction. The JVM cannot distinguish
a thread executing on CPU vs one in the OS ready queue — that's the OS scheduler's job.
Even threads doing blocking I/O show as RUNNABLE, because the JVM doesn't model OS-level
I/O blocking.

---

## Quick Reference Cheat Sheet

```java
// Creating threads
Thread t = new Thread(() -> doWork());
Thread t = new Thread(myRunnable, "ThreadName");

// Lifecycle
t.start();           // NEW → RUNNABLE
t.join();            // caller waits for t to finish
t.join(1000);        // caller waits at most 1000ms

// State inspection
t.getState();        // returns Thread.State enum
t.isAlive();         // true if started and not yet terminated

// Priority (hint only)
t.setPriority(Thread.MAX_PRIORITY);  // 10
t.setPriority(Thread.NORM_PRIORITY); // 5 (default)
t.setPriority(Thread.MIN_PRIORITY);  // 1

// Daemon
t.setDaemon(true);   // before start()!
t.isDaemon();

// Interruption
t.interrupt();                         // set flag or throw InterruptedException
t.isInterrupted();                     // check flag (don't clear)
Thread.interrupted();                  // check AND clear flag (static)

// Exception handling
t.setUncaughtExceptionHandler((th, ex) -> log(ex));
Thread.setDefaultUncaughtExceptionHandler((th, ex) -> log(ex));

// Utility
Thread.currentThread();
Thread.getAllStackTraces();
```

### State Triggers Quick Reference

```
NEW → RUNNABLE               : start()
RUNNABLE → BLOCKED            : enter synchronized (lock held by other)
BLOCKED → RUNNABLE            : monitor lock acquired
RUNNABLE → WAITING            : wait(), join(), LockSupport.park()
WAITING → RUNNABLE            : notify(), notifyAll(), unpark(), join target dies
RUNNABLE → TIMED_WAITING      : sleep(ms), wait(ms), join(ms), parkNanos()
TIMED_WAITING → RUNNABLE      : timeout, notify(), unpark()
RUNNABLE → TERMINATED         : run() returns or uncaught exception
```

