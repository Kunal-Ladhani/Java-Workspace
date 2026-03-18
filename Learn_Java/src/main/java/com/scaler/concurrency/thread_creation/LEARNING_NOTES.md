# Thread Creation in Java — Complete Guide for SDE2

## Table of Contents

1. [Why Multiple Ways to Create Threads?](#1-why-multiple-ways-to-create-threads)
2. [Way 1: Extending Thread Class](#2-way-1-extending-thread-class)
3. [Way 2: Implementing Runnable Interface](#3-way-2-implementing-runnable-interface)
4. [Way 3: Implementing Callable\<V\> Interface](#4-way-3-implementing-callablev-interface)
5. [Way 4: Using FutureTask](#5-way-4-using-futuretask)
6. [Way 5: Using ExecutorService (Thread Pools)](#6-way-5-using-executorservice-thread-pools)
7. [Way 6: Lambda Expressions (Java 8+)](#7-way-6-lambda-expressions-java-8)
8. [Detailed Comparison Table](#8-detailed-comparison-table)
9. [Runnable vs Callable — Deep Dive](#9-runnable-vs-callable--deep-dive)
10. [Thread Factory](#10-thread-factory)
11. [Best Practices](#11-best-practices)
12. [SDE2 Interview Questions & Answers](#12-sde2-interview-questions--answers)

---

## 1. Why Multiple Ways to Create Threads?

Java has evolved over 25+ years, and each new mechanism for thread creation was introduced to
address limitations of the previous approach.

### Timeline of evolution

| Java Version | What was introduced                     | Why                                               |
|-------------|------------------------------------------|----------------------------------------------------|
| JDK 1.0     | `Thread` class                           | The original and only way to create threads        |
| JDK 1.0     | `Runnable` interface                     | Separates task from thread; allows extending other classes |
| JDK 1.5     | `Callable<V>`, `Future<V>`, `ExecutorService` | Return values, checked exceptions, thread pooling  |
| JDK 1.5     | `FutureTask`                             | Bridge between Callable and Thread                 |
| JDK 1.5     | `ThreadFactory`                          | Standardized thread creation configuration         |
| JDK 1.8     | Lambda expressions                       | Cleaner syntax for Runnable/Callable               |
| JDK 1.8     | `CompletableFuture`                      | Composable, non-blocking async programming         |

### The key insight

Each mechanism doesn't replace the previous one — they build on top of each other:

```
Thread class  →  Runnable (separation of concerns)
                     ↓
              Callable + Future (return values + exceptions)
                     ↓
              ExecutorService (thread pool management)
                     ↓
              CompletableFuture (composable async pipelines)
```

**For interviews**: know all of them, but in production code, you'll almost always use
`ExecutorService` with `Runnable`/`Callable`, or `CompletableFuture`.

---

## 2. Way 1: Extending Thread Class

The most basic approach — create a subclass of `java.lang.Thread` and override `run()`.

### How it works

```java
class MyThread extends Thread {
    @Override
    public void run() {
        System.out.println("Running in: " + Thread.currentThread().getName());
    }
}

// Usage
MyThread thread = new MyThread();
thread.start();  // creates a new OS thread, invokes run()
```

### What happens when you call start()?

1. JVM allocates a new **native OS thread** (via `pthread_create` on Linux/macOS).
2. The new thread's execution begins at the `run()` method.
3. `start()` returns immediately — the calling thread does **not** wait.

### CRITICAL: start() vs run()

```java
MyThread t = new MyThread();
t.run();    // ❌ Runs in the CURRENT thread (no new thread created!)
t.start();  // ✅ Creates a new thread and runs run() in it
```

Calling `run()` directly is a **common bug** — it just invokes the method like any normal
method call on the current thread. No concurrency happens.

### Limitations

1. **Single inheritance**: Java doesn't support multiple inheritance. If you extend `Thread`,
   you **cannot extend any other class**.

   ```java
   class MyTask extends Thread {  // already extends Thread
       // CANNOT also: extends SomeBaseClass  ← compile error
   }
   ```

2. **Tight coupling**: The task logic is embedded inside the Thread subclass. You can't
   reuse the same task logic with a different execution mechanism (like a thread pool).

3. **Inflexible**: You can't submit a Thread subclass to an `ExecutorService`.

### When to use

**Rarely.** The only legitimate reason is if you need to override other `Thread` methods
like `interrupt()`, `toString()`, or use thread-specific storage. In practice, this is
almost never needed.

### Thread lifecycle when extending Thread

```
NEW  ──start()──>  RUNNABLE  ──scheduler──>  RUNNING
                                                 │
                                     ┌───────────┴───────────┐
                                     ↓                       ↓
                                 BLOCKED/                TERMINATED
                                 WAITING                  (run() completed
                                 TIMED_WAITING             or exception)
```

---

## 3. Way 2: Implementing Runnable Interface

The **preferred** way to define a task that a thread will execute.

### The Runnable interface

```java
@FunctionalInterface
public interface Runnable {
    void run();
}
```

Key points:
- It's a `@FunctionalInterface` (single abstract method) → lambda-compatible.
- The `run()` method returns `void` and cannot throw checked exceptions.

### How to use

```java
// Approach A: Named class
class MyTask implements Runnable {
    @Override
    public void run() {
        System.out.println("Task running in: " + Thread.currentThread().getName());
    }
}

Thread thread = new Thread(new MyTask());
thread.start();

// Approach B: Anonymous class
Thread thread = new Thread(new Runnable() {
    @Override
    public void run() {
        System.out.println("Anonymous task");
    }
});
thread.start();

// Approach C: Lambda (Java 8+)
Thread thread = new Thread(() -> System.out.println("Lambda task"));
thread.start();
```

### Why Runnable is better than extending Thread

| Aspect                  | Thread class               | Runnable                     |
|-------------------------|----------------------------|------------------------------|
| Inheritance             | Uses up your one superclass| Free to extend another class |
| Design principle        | Violates SRP               | Follows SRP (task ≠ thread)  |
| Reusability             | Task tied to Thread        | Same Runnable, many threads  |
| Executor compatibility  | No                         | Yes                          |
| Testability             | Harder (need to start thread)| Easy (just call run())     |

### Sharing a Runnable across multiple threads

```java
Runnable sharedTask = () -> {
    String name = Thread.currentThread().getName();
    System.out.println(name + " executing shared task");
};

new Thread(sharedTask, "Worker-1").start();
new Thread(sharedTask, "Worker-2").start();
new Thread(sharedTask, "Worker-3").start();
```

This is **not possible** with the Thread class approach — you'd need separate instances.

### Runnable with ExecutorService

```java
ExecutorService executor = Executors.newFixedThreadPool(4);

executor.execute(() -> System.out.println("Task 1"));  // fire-and-forget
executor.submit(() -> System.out.println("Task 2"));   // returns Future<?>

executor.shutdown();
```

---

## 4. Way 3: Implementing Callable\<V\> Interface

Introduced in **Java 5** to solve two limitations of `Runnable`:
1. `Runnable.run()` returns `void` — no way to return a result.
2. `Runnable.run()` cannot throw **checked** exceptions.

### The Callable interface

```java
@FunctionalInterface
public interface Callable<V> {
    V call() throws Exception;
}
```

### How it works

Callable **cannot** be passed directly to `Thread` (Thread only accepts Runnable).
You must use it with `ExecutorService` or wrap it in a `FutureTask`.

```java
Callable<Integer> task = () -> {
    Thread.sleep(1000);  // can throw checked exception — no try-catch needed!
    return 42;
};

ExecutorService executor = Executors.newSingleThreadExecutor();
Future<Integer> future = executor.submit(task);

// Do other work while task runs...

Integer result = future.get();  // blocks until result is available
System.out.println("Result: " + result);  // 42

executor.shutdown();
```

### The Future\<V\> interface

`Future<V>` represents the **pending result** of an asynchronous computation.

```java
public interface Future<V> {
    boolean cancel(boolean mayInterruptIfRunning);
    boolean isCancelled();
    boolean isDone();
    V get() throws InterruptedException, ExecutionException;
    V get(long timeout, TimeUnit unit) throws InterruptedException,
                                               ExecutionException,
                                               TimeoutException;
}
```

#### Method-by-method breakdown

| Method                         | Behavior                                                    |
|--------------------------------|-------------------------------------------------------------|
| `get()`                        | Blocks indefinitely until result is ready                   |
| `get(timeout, unit)`           | Blocks up to timeout; throws `TimeoutException` if expired  |
| `isDone()`                     | Returns `true` if task completed (normally, exception, or cancelled) |
| `isCancelled()`                | Returns `true` if task was cancelled before completion      |
| `cancel(mayInterrupt)`         | Attempts to cancel; if `true`, interrupts running task      |

#### Exception handling with Future

If the `Callable` throws an exception, it is **wrapped** in an `ExecutionException`
and re-thrown when you call `future.get()`:

```java
Callable<Integer> failingTask = () -> {
    throw new IOException("Network error");
};

Future<Integer> future = executor.submit(failingTask);

try {
    future.get();
} catch (ExecutionException e) {
    Throwable cause = e.getCause();  // the original IOException
    System.out.println("Task failed: " + cause.getMessage());
}
```

This is a **major advantage** over Runnable, where exceptions thrown in `run()` are
silently swallowed (or handled by `UncaughtExceptionHandler`).

---

## 5. Way 4: Using FutureTask

`FutureTask` is a concrete class that implements **both** `Runnable` and `Future<V>`.
It bridges the gap between `Callable` and `Thread`.

### Why does FutureTask exist?

- `Thread` only accepts `Runnable`.
- `Callable` is not `Runnable`.
- `FutureTask` wraps a `Callable` and makes it `Runnable` — so you can pass it to `Thread`.

```
Callable ──wraps──> FutureTask (implements Runnable + Future)
                         │
                    pass to Thread
                         │
                    thread.start()
                         │
                    futureTask.get()  ← get the result
```

### Usage

```java
Callable<String> callable = () -> {
    Thread.sleep(500);
    return "Hello from Callable via FutureTask!";
};

FutureTask<String> futureTask = new FutureTask<>(callable);

Thread thread = new Thread(futureTask);
thread.start();

// Do other work...

String result = futureTask.get();  // blocks until done
System.out.println(result);
```

### FutureTask can also wrap a Runnable

```java
Runnable runnable = () -> System.out.println("Runnable task");
FutureTask<String> futureTask = new FutureTask<>(runnable, "completed");

new Thread(futureTask).start();
String result = futureTask.get();  // returns "completed"
```

### When to use FutureTask

- When you need a **result** from a thread but can't use `ExecutorService`
  (rare, but possible in framework code or custom schedulers).
- When you want to share a computation: multiple threads can call `get()` on the same
  `FutureTask` — it runs the computation **only once** and caches the result.

### FutureTask guarantees

- The `call()` method runs **exactly once**, even if multiple threads call `run()`.
- After completion, `get()` returns immediately with the cached result.
- Thread-safe: multiple threads can safely call `get()` concurrently.

---

## 6. Way 5: Using ExecutorService (Thread Pools)

In production code, you should **never** create threads manually. Use thread pools instead.

### Why thread pools?

Creating a thread is **expensive**:
- Each thread allocates ~512KB–1MB of stack memory.
- Thread creation involves a system call (`pthread_create`) — OS-level overhead.
- Too many threads cause context-switching overhead and potential `OutOfMemoryError`.

Thread pools solve this by **reusing** a fixed set of threads:

```
Task Queue:  [Task1] [Task2] [Task3] [Task4] [Task5]
                 ↓       ↓       ↓
Thread Pool: [Thread-1] [Thread-2] [Thread-3]
             (reused)   (reused)    (reused)
```

### Common pool types

```java
// Fixed pool: exactly N threads. Best for known workloads.
ExecutorService fixed = Executors.newFixedThreadPool(4);

// Cached pool: creates threads as needed, reuses idle ones (60s timeout).
// Good for many short-lived tasks. Risky for unbounded workloads.
ExecutorService cached = Executors.newCachedThreadPool();

// Single thread: one thread, tasks execute sequentially.
// Guarantees task ordering (FIFO).
ExecutorService single = Executors.newSingleThreadExecutor();

// Scheduled: supports delayed and periodic execution.
ScheduledExecutorService scheduled = Executors.newScheduledThreadPool(2);
```

### Submitting tasks

```java
ExecutorService executor = Executors.newFixedThreadPool(3);

// 1. execute(Runnable) — fire and forget, no return value
executor.execute(() -> System.out.println("fire-and-forget"));

// 2. submit(Runnable) — returns Future<?> (result is null)
Future<?> f1 = executor.submit(() -> System.out.println("submitted runnable"));

// 3. submit(Callable<V>) — returns Future<V> with the result
Future<Integer> f2 = executor.submit(() -> 42);
Integer result = f2.get();  // 42

// 4. submit(Runnable, T result) — returns Future<T> with the given result
Future<String> f3 = executor.submit(() -> System.out.println("task"), "done");
String res = f3.get();  // "done"
```

### Shutting down properly

```java
executor.shutdown();          // stops accepting new tasks, runs queued tasks
executor.shutdownNow();       // interrupts running tasks, returns unstarted tasks
executor.awaitTermination(5, TimeUnit.SECONDS);  // blocks until all tasks complete
```

**Always shut down your executor.** Without `shutdown()`, the JVM will not exit because
the pool threads are non-daemon by default.

### execute() vs submit()

| Aspect          | `execute(Runnable)`     | `submit(Runnable/Callable)` |
|-----------------|-------------------------|-----------------------------|
| Return value    | `void`                  | `Future<?> / Future<V>`     |
| Exception handling | Uncaught exceptions crash the thread | Exceptions captured in Future |
| Use case        | Fire-and-forget         | Need result or exception handling |

---

## 7. Way 6: Lambda Expressions (Java 8+)

Since both `Runnable` and `Callable` are `@FunctionalInterface`, you can use lambdas.

### Runnable lambda

```java
// Before Java 8
new Thread(new Runnable() {
    @Override
    public void run() {
        System.out.println("Old way");
    }
}).start();

// Java 8+
new Thread(() -> System.out.println("Lambda way")).start();
```

### Callable lambda

```java
// Before Java 8
Callable<Integer> task = new Callable<Integer>() {
    @Override
    public Integer call() throws Exception {
        return 42;
    }
};

// Java 8+
Callable<Integer> task = () -> 42;
```

### Multi-line lambdas

```java
new Thread(() -> {
    System.out.println("Step 1");
    System.out.println("Step 2");
    System.out.println("Running in: " + Thread.currentThread().getName());
}).start();
```

### Method references

```java
// If a method matches the Runnable signature: void methodName()
new Thread(SomeClass::doWork).start();
```

### Lambdas are not a separate mechanism

Lambdas are **syntactic sugar** for Runnable/Callable — they don't create threads
differently. Under the hood, the JVM generates an anonymous class using `invokedynamic`.

---

## 8. Detailed Comparison Table

| Feature                  | Thread class  | Runnable      | Callable\<V\>  | FutureTask      |
|--------------------------|---------------|---------------|-----------------|-----------------|
| **Type**                 | Class         | Interface     | Interface       | Class           |
| **Method to implement**  | `run()`       | `run()`       | `call()`        | wraps Callable  |
| **Return value**         | `void`        | `void`        | `V` (generic)   | `V` (via get()) |
| **Checked exceptions**   | No            | No            | Yes             | Yes (via get()) |
| **Can extend other class** | No          | Yes           | Yes             | N/A             |
| **Pass to Thread directly** | Yes (is a Thread) | Yes    | No              | Yes (is Runnable) |
| **Used with ExecutorService** | No       | Yes           | Yes             | Yes             |
| **Lambda support**       | No            | Yes           | Yes             | N/A             |
| **Get result**           | Not possible  | Not possible  | Via Future      | Via get()       |
| **Introduced in**        | JDK 1.0       | JDK 1.0       | JDK 1.5         | JDK 1.5         |

### Quick decision guide

```
Need a result from the thread?
├── YES → Use Callable<V> + ExecutorService (or FutureTask)
└── NO
    ├── Production code? → Use ExecutorService + Runnable
    └── Learning/simple script? → new Thread(() -> ...).start()
```

---

## 9. Runnable vs Callable — Deep Dive

### Side-by-side comparison

```java
// Runnable: no return, no checked exception
Runnable runnable = () -> {
    System.out.println("I can't return anything");
    // throw new IOException("nope");  ← compile error!
};

// Callable: returns a value, can throw checked exception
Callable<String> callable = () -> {
    if (someCondition) throw new IOException("failed");
    return "success";
};
```

### Exception handling comparison

**Runnable**: exceptions in `run()` are **lost** unless you catch them yourself:

```java
Runnable task = () -> {
    throw new RuntimeException("Oops!");
    // This exception goes to UncaughtExceptionHandler (default: printed to stderr)
    // The caller has NO way to catch it.
};

Thread t = new Thread(task);
t.setUncaughtExceptionHandler((thread, ex) -> {
    System.out.println("Thread " + thread.getName() + " failed: " + ex.getMessage());
});
t.start();
```

**Callable**: exceptions are **captured** in the `Future` and re-thrown on `get()`:

```java
Callable<String> task = () -> {
    throw new IOException("Network error");
};

Future<String> future = executor.submit(task);

try {
    String result = future.get();
} catch (ExecutionException e) {
    // e.getCause() → the original IOException
    System.out.println("Caught: " + e.getCause().getMessage());
}
```

This is the **#1 reason** to prefer Callable when failure handling matters.

### Converting between Runnable and Callable

```java
// Runnable → Callable (with a predefined result)
Runnable runnable = () -> System.out.println("task");
Callable<String> callable = Executors.callable(runnable, "done");

// Callable → Runnable (lose the return value)
Callable<Integer> myCallable = () -> 42;
FutureTask<Integer> futureTask = new FutureTask<>(myCallable);
Runnable asRunnable = futureTask;  // FutureTask implements Runnable
```

### CompletableFuture — The Modern Alternative (Java 8+)

`CompletableFuture` is the evolution of `Future`. It supports:
- **Non-blocking** result handling (callbacks instead of blocking `get()`)
- **Chaining** operations: `thenApply`, `thenCompose`, `thenAccept`
- **Combining** multiple futures: `allOf`, `anyOf`
- **Exception handling**: `exceptionally`, `handle`

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    return "Hello";
}).thenApply(result -> {
    return result + " World";
}).thenApply(String::toUpperCase);

future.thenAccept(System.out::println);  // "HELLO WORLD" — non-blocking!
```

**For interviews**: mention `CompletableFuture` as the modern approach, but know
`Callable`/`Future` thoroughly — that's what interviewers usually test.

---

## 10. Thread Factory

`ThreadFactory` is a simple interface for creating threads with consistent configuration.

### The interface

```java
public interface ThreadFactory {
    Thread newThread(Runnable r);
}
```

### Why use ThreadFactory?

Without it, every place that creates a thread must manually set the same configuration:

```java
// Without ThreadFactory — repeated boilerplate
Thread t1 = new Thread(task);
t1.setName("worker-1");
t1.setDaemon(true);
t1.setPriority(Thread.NORM_PRIORITY);
t1.setUncaughtExceptionHandler(handler);

Thread t2 = new Thread(task);
t2.setName("worker-2");
t2.setDaemon(true);
t2.setPriority(Thread.NORM_PRIORITY);
t2.setUncaughtExceptionHandler(handler);
```

### Custom ThreadFactory

```java
public class NamedDaemonThreadFactory implements ThreadFactory {
    private final String prefix;
    private final AtomicInteger counter = new AtomicInteger(1);

    public NamedDaemonThreadFactory(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setName(prefix + "-" + counter.getAndIncrement());
        thread.setDaemon(true);
        thread.setPriority(Thread.NORM_PRIORITY);
        thread.setUncaughtExceptionHandler((t, e) -> {
            System.err.println("Thread " + t.getName() + " failed: " + e.getMessage());
        });
        return thread;
    }
}

// Usage with ExecutorService
ExecutorService executor = Executors.newFixedThreadPool(4,
    new NamedDaemonThreadFactory("my-worker"));
```

### Default ThreadFactory

```java
ThreadFactory defaultFactory = Executors.defaultThreadFactory();
Thread t = defaultFactory.newThread(() -> System.out.println("Hello"));
// Creates: Thread[pool-1-thread-1, 5, main]
```

### ThreadFactory in practice

- **Naming**: Makes thread dumps readable (`"order-processor-3"` vs `"Thread-17"`).
- **Daemon threads**: Ensure threads don't prevent JVM shutdown.
- **Priority**: Set appropriate priority for background tasks.
- **Exception handling**: Centralized uncaught exception logging.
- Used by: `ExecutorService`, `ForkJoinPool`, `ScheduledThreadPoolExecutor`.

---

## 11. Best Practices

### 1. Prefer Runnable over Thread (Composition over Inheritance)

```java
// ❌ Bad: extends Thread — uses up your inheritance slot
class DataProcessor extends Thread {
    public void run() { /* process data */ }
}

// ✅ Good: implements Runnable — can still extend other classes
class DataProcessor extends BaseProcessor implements Runnable {
    public void run() { /* process data */ }
}
```

### 2. Prefer Callable when you need a result

```java
// ❌ Bad: Using shared mutable state to "return" a result from Runnable
class ResultHolder { volatile String result; }
ResultHolder holder = new ResultHolder();
new Thread(() -> holder.result = "done").start();

// ✅ Good: Use Callable — the result is part of the API contract
Future<String> future = executor.submit(() -> "done");
String result = future.get();
```

### 3. Prefer ExecutorService over manual thread creation

```java
// ❌ Bad: Creating threads manually (expensive, no reuse, no bounds)
for (int i = 0; i < 1000; i++) {
    new Thread(() -> processRequest()).start();  // 1000 threads!
}

// ✅ Good: Thread pool bounds concurrency, reuses threads
ExecutorService executor = Executors.newFixedThreadPool(10);
for (int i = 0; i < 1000; i++) {
    executor.submit(() -> processRequest());  // only 10 threads
}
executor.shutdown();
```

### 4. Always shut down your ExecutorService

```java
ExecutorService executor = Executors.newFixedThreadPool(4);
try {
    // submit tasks...
} finally {
    executor.shutdown();
    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
        executor.shutdownNow();
    }
}
```

`shutdown()` vs `shutdownNow()`:
- `shutdown()`: Graceful — completes running and queued tasks, rejects new ones.
- `shutdownNow()`: Aggressive — interrupts running tasks, returns unstarted ones.

### 5. Name your threads

```java
// ❌ Thread dump shows: "Thread-0", "Thread-1" — useless for debugging
new Thread(task).start();

// ✅ Thread dump shows: "order-processor-1" — instantly meaningful
new Thread(task, "order-processor-1").start();

// ✅ Even better: use a ThreadFactory
ExecutorService executor = Executors.newFixedThreadPool(4,
    new NamedDaemonThreadFactory("order-processor"));
```

### 6. Handle InterruptedException correctly

```java
// ❌ Bad: Swallowing the interrupt
try {
    Thread.sleep(1000);
} catch (InterruptedException e) {
    // silently ignored — the interrupt flag is now cleared!
}

// ✅ Good: Restore the interrupt flag
try {
    Thread.sleep(1000);
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();  // restore flag
    return;  // exit gracefully
}
```

### 7. Prefer try-with-resources for ExecutorService (Java 19+)

```java
// Java 19+: ExecutorService implements AutoCloseable
try (ExecutorService executor = Executors.newFixedThreadPool(4)) {
    executor.submit(() -> doWork());
}  // automatically shuts down
```

---

## 12. SDE2 Interview Questions & Answers

### Q1: What are the different ways to create a thread in Java?

**Answer**: There are six main approaches:

1. **Extend `Thread` class** — override `run()`, call `start()`.
2. **Implement `Runnable`** — implement `run()`, pass to `new Thread(runnable).start()`.
3. **Implement `Callable<V>`** — implement `call()`, submit to `ExecutorService`,
   get result via `Future<V>`.
4. **Use `FutureTask`** — wraps Callable, implements both Runnable and Future, can be
   passed directly to Thread.
5. **Use `ExecutorService`** — thread pool that manages threads; submit Runnable/Callable.
6. **Lambda expressions** — syntactic sugar for Runnable/Callable since Java 8.

---

### Q2: Why should you prefer Runnable over extending Thread?

**Answer**: Three reasons:

1. **Inheritance flexibility**: Java has single inheritance. Implementing Runnable leaves
   your extends slot open for a domain class.
2. **Separation of concerns**: Runnable separates the *task* from the *execution mechanism*.
   The same Runnable can run in a Thread, an ExecutorService, or a ForkJoinPool.
3. **Reusability**: A single Runnable instance can be shared across multiple threads.
   A Thread subclass instance can only be started once.

This follows the principle of **composition over inheritance**.

---

### Q3: What is the difference between start() and run()?

**Answer**:

- `start()`: Creates a **new native OS thread** and invokes `run()` on that new thread.
  It returns immediately (non-blocking). Can only be called **once** per Thread object;
  calling it again throws `IllegalThreadStateException`.

- `run()`: Just a **normal method call** on the current thread. No new thread is created.
  Calling `run()` directly defeats the purpose of threading.

```java
Thread t = new Thread(() -> System.out.println(Thread.currentThread().getName()));
t.run();    // prints "main" — runs on the calling thread
t.start();  // prints "Thread-0" — runs on a new thread
```

---

### Q4: What is Future? How do you get a result from a thread?

**Answer**: `Future<V>` represents the pending result of an asynchronous computation.

- `future.get()` blocks until the result is available.
- `future.get(timeout, unit)` blocks up to the timeout.
- `future.isDone()` checks if the task has completed.
- `future.cancel(mayInterrupt)` attempts to cancel the task.

To get a result from a thread:
1. Use `Callable<V>` + `ExecutorService.submit()` → returns `Future<V>`.
2. Or wrap `Callable` in `FutureTask`, pass to Thread, call `futureTask.get()`.

If the callable threw an exception, `future.get()` throws `ExecutionException` wrapping
the original exception.

---

### Q5: What is the difference between Runnable and Callable?

**Answer**:

| Aspect              | Runnable            | Callable\<V\>          |
|---------------------|---------------------|------------------------|
| Method              | `run()`             | `call()`               |
| Return value        | `void`              | `V` (generic type)     |
| Checked exceptions  | Cannot throw        | Can throw `Exception`  |
| Pass to Thread      | Yes                 | No (need FutureTask)   |
| Pass to Executor    | Yes                 | Yes                    |
| Introduced in       | JDK 1.0             | JDK 1.5                |

Use Runnable for fire-and-forget tasks. Use Callable when you need a result or need to
propagate checked exceptions.

---

### Q6: What is FutureTask and why does it exist?

**Answer**: `FutureTask` implements both `Runnable` and `Future<V>`. It exists to bridge
the gap between `Callable` and `Thread`:

- `Thread` only accepts `Runnable`.
- `Callable` doesn't implement `Runnable`.
- `FutureTask` wraps a `Callable`, making it a `Runnable` that you can pass to `Thread`,
  while also providing `Future.get()` to retrieve the result.

```java
FutureTask<Integer> ft = new FutureTask<>(() -> 42);
new Thread(ft).start();
int result = ft.get();  // 42
```

It guarantees the computation runs exactly once and caches the result.

---

### Q7: Why use thread pools instead of creating threads manually?

**Answer**: Thread creation is expensive because:

1. Each thread allocates **~512KB–1MB** of stack memory.
2. Thread creation involves a **system call** to the OS.
3. Too many threads cause **context-switching overhead**.
4. Unbounded thread creation can cause `OutOfMemoryError`.

Thread pools (via `ExecutorService`) solve this by:
- **Reusing** threads — amortizing creation cost.
- **Bounding** concurrency — preventing resource exhaustion.
- **Managing** lifecycle — graceful shutdown, task queuing.
- **Separating** submission from execution — cleaner architecture.

---

### Q8: What is the difference between execute() and submit() in ExecutorService?

**Answer**:

| Aspect              | `execute(Runnable)` | `submit(Runnable/Callable)` |
|---------------------|---------------------|-----------------------------|
| Return value        | `void`              | `Future<?> / Future<V>`     |
| Exception handling  | Exceptions crash the thread, go to UncaughtExceptionHandler | Exceptions captured in Future, thrown on `get()` |
| Accepts Callable?   | No                  | Yes                         |

Use `execute()` for fire-and-forget. Use `submit()` when you need the result or want
to handle exceptions through the `Future`.

---

### Q9: Can you start a thread twice? What happens?

**Answer**: **No.** Calling `start()` on a thread that has already been started (or has
finished) throws `IllegalThreadStateException`.

A `Thread` object goes through states: NEW → RUNNABLE → TERMINATED. Once it leaves NEW,
`start()` cannot be called again. If you need to run the same task again, create a new
`Thread` object (or better yet, use a thread pool that reuses threads internally).

---

### Q10: How does exception handling differ between Runnable and Callable?

**Answer**:

**Runnable**: Exceptions thrown in `run()` propagate to the thread's
`UncaughtExceptionHandler`. If none is set, the exception is printed to `System.err`
and the thread dies silently. The caller (e.g., the thread that called `start()`)
**cannot catch these exceptions**.

**Callable**: Exceptions thrown in `call()` are captured by the `Future`. When
you call `future.get()`, the exception is re-thrown as an `ExecutionException`.
The caller can catch it and inspect `e.getCause()` for the original exception.

```java
// Callable exception handling
try {
    future.get();
} catch (ExecutionException e) {
    Throwable original = e.getCause();
    // handle the original exception
}
```

This makes Callable **much better** for error handling in concurrent code.

---

### Summary: What to Remember for Interviews

1. **6 ways** to create threads — know them all with code examples.
2. **Prefer Runnable** over Thread (composition over inheritance).
3. **Callable** = Runnable + return value + checked exceptions.
4. **Future** = handle for async result; `get()` blocks.
5. **FutureTask** = bridges Callable to Thread.
6. **ExecutorService** = always use in production (never raw threads).
7. **start() vs run()** — start() creates a new thread; run() doesn't.
8. **execute() vs submit()** — submit() returns Future; execute() doesn't.
9. **shutdown()** your executor — or JVM won't exit.
10. **CompletableFuture** = the modern non-blocking alternative to Future.
