package com.scaler.concurrency.thread_creation;

import java.io.IOException;
import java.util.concurrent.*;

/**
 * Focused demo showing the practical differences between Runnable and Callable:
 *   1. Runnable: no return value, no checked exceptions
 *   2. Callable: returns a value, throws checked exceptions
 *   3. Exception handling comparison (how Callable propagates via Future.get())
 *   4. Future API methods: isDone(), get(), get(timeout), cancel()
 *
 * Run main() and observe the console output.
 */
public class CallableVsRunnableDemo {

    public static void main(String[] args) throws Exception {

        System.out.println("========================================");
        System.out.println("  SECTION 1: Runnable — No Return Value");
        System.out.println("========================================");
        demoRunnableNoReturn();

        Thread.sleep(500);

        System.out.println("\n========================================");
        System.out.println("  SECTION 2: Callable — With Return Value");
        System.out.println("========================================");
        demoCallableWithReturn();

        Thread.sleep(500);

        System.out.println("\n========================================");
        System.out.println("  SECTION 3: Exception Handling Comparison");
        System.out.println("========================================");
        demoExceptionHandling();

        Thread.sleep(500);

        System.out.println("\n========================================");
        System.out.println("  SECTION 4: Future API — isDone(), get(),");
        System.out.println("             get(timeout), cancel()");
        System.out.println("========================================");
        demoFutureApi();
    }

    // ──────────────────────────────────────────────
    //  SECTION 1: Runnable cannot return a value
    // ──────────────────────────────────────────────
    private static void demoRunnableNoReturn() throws InterruptedException, ExecutionException {
        System.out.println("\n--- Problem: How to get a result from a Runnable? ---");

        // Workaround 1: Shared mutable state (fragile, not recommended)
        final String[] resultHolder = new String[1];

        Runnable task = () -> {
            System.out.println("  [Runnable] Computing...");
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            resultHolder[0] = "Computed Result";
            System.out.println("  [Runnable] Done, but run() returns void.");
        };

        Thread thread = new Thread(task, "RunnableWorker");
        thread.start();
        thread.join();

        System.out.println("  Workaround: used shared array to extract result = \"" + resultHolder[0] + "\"");
        System.out.println("  This is fragile — requires careful synchronization in real code.");

        // Workaround 2: Submit Runnable to executor, get Future<?> with null result
        System.out.println("\n--- Workaround 2: executor.submit(Runnable) returns Future<?> ---");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> {
            System.out.println("  [Runnable via Executor] Running...");
        });

        Object result = future.get();
        System.out.println("  future.get() = " + result + "  (always null for Runnable)");

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("\n  TAKEAWAY: Runnable was NOT designed to return results.");
        System.out.println("  Use Callable<V> when you need a return value.\n");
    }

    // ──────────────────────────────────────────────
    //  SECTION 2: Callable returns a value cleanly
    // ──────────────────────────────────────────────
    private static void demoCallableWithReturn() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        System.out.println("\n--- Callable returning an Integer ---");
        Callable<Integer> sumTask = () -> {
            System.out.println("  [Callable-Sum] Computing sum of 1..100");
            int sum = 0;
            for (int i = 1; i <= 100; i++) sum += i;
            return sum;
        };

        Future<Integer> sumFuture = executor.submit(sumTask);
        System.out.println("  Result: " + sumFuture.get());

        System.out.println("\n--- Callable returning a String ---");
        Callable<String> greetTask = () -> {
            System.out.println("  [Callable-Greet] Building greeting...");
            Thread.sleep(200);
            return "Hello from Callable!";
        };

        Future<String> greetFuture = executor.submit(greetTask);
        System.out.println("  Result: \"" + greetFuture.get() + "\"");

        System.out.println("\n--- Callable with checked exception (no try-catch in call()) ---");
        Callable<String> ioTask = () -> {
            // Callable.call() throws Exception — checked exceptions are part of the contract
            System.out.println("  [Callable-IO] Simulating file read...");
            Thread.sleep(100);
            return "File contents read successfully";
        };

        Future<String> ioFuture = executor.submit(ioTask);
        System.out.println("  Result: \"" + ioFuture.get() + "\"");

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("\n  TAKEAWAY: Callable<V>.call() returns V and can throw Exception.");
        System.out.println("  The return value is obtained via Future<V>.get().\n");
    }

    // ──────────────────────────────────────────────
    //  SECTION 3: Exception Handling Comparison
    // ──────────────────────────────────────────────
    private static void demoExceptionHandling() throws InterruptedException {

        // --- Runnable: exception goes to UncaughtExceptionHandler ---
        System.out.println("\n--- Runnable: Exception goes to UncaughtExceptionHandler ---");

        Thread runnableThread = new Thread(() -> {
            System.out.println("  [Runnable] About to throw RuntimeException...");
            throw new RuntimeException("Runnable failed!");
        }, "FailingRunnable");

        runnableThread.setUncaughtExceptionHandler((t, e) -> {
            System.out.println("  [UncaughtExceptionHandler] Thread \"" + t.getName()
                    + "\" threw: " + e.getClass().getSimpleName() + " — " + e.getMessage());
        });

        runnableThread.start();
        runnableThread.join();

        System.out.println("  The calling thread (main) has NO direct way to catch that exception.");

        // --- Callable: exception is captured in Future ---
        System.out.println("\n--- Callable: Exception captured in Future, thrown on get() ---");

        ExecutorService executor = Executors.newSingleThreadExecutor();

        Callable<String> failingCallable = () -> {
            System.out.println("  [Callable] About to throw IOException...");
            throw new IOException("Callable failed: network error");
        };

        Future<String> future = executor.submit(failingCallable);

        try {
            String result = future.get();
            System.out.println("  Result: " + result);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            System.out.println("  [main] Caught ExecutionException on future.get()");
            System.out.println("  [main] Original exception: " + cause.getClass().getSimpleName()
                    + " — " + cause.getMessage());
            System.out.println("  [main] The calling thread CAN catch and handle this!");
        }

        // --- Callable: RuntimeException also captured ---
        System.out.println("\n--- Callable: RuntimeException also captured in Future ---");

        Future<Integer> future2 = executor.submit(() -> {
            System.out.println("  [Callable] About to throw ArithmeticException...");
            return 10 / 0;
        });

        try {
            future2.get();
        } catch (ExecutionException e) {
            System.out.println("  [main] Caught: " + e.getCause().getClass().getSimpleName()
                    + " — " + e.getCause().getMessage());
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("\n  TAKEAWAY:");
        System.out.println("  - Runnable exceptions: go to UncaughtExceptionHandler, caller can't catch.");
        System.out.println("  - Callable exceptions: wrapped in ExecutionException, caller catches on get().");
        System.out.println("  - This is why Callable is preferred when error handling matters.\n");
    }

    // ──────────────────────────────────────────────
    //  SECTION 4: Future API Demo
    // ──────────────────────────────────────────────
    private static void demoFutureApi() throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // --- isDone() demo ---
        System.out.println("\n--- 4a: isDone() — Check if task has completed ---");

        Future<String> slowFuture = executor.submit(() -> {
            Thread.sleep(1000);
            return "slow result";
        });

        System.out.println("  Immediately after submit: isDone() = " + slowFuture.isDone());
        Thread.sleep(1200);
        System.out.println("  After 1.2 seconds:        isDone() = " + slowFuture.isDone());
        System.out.println("  Result: \"" + slowFuture.get() + "\"");

        // --- get(timeout) demo ---
        System.out.println("\n--- 4b: get(timeout, unit) — Timeout if task takes too long ---");

        Future<String> longTask = executor.submit(() -> {
            Thread.sleep(3000);
            return "long result";
        });

        try {
            System.out.println("  Calling get(1, SECONDS) on a 3-second task...");
            longTask.get(1, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            System.out.println("  TimeoutException! Task didn't complete within 1 second.");
            System.out.println("  isDone() = " + longTask.isDone() + "  (task is still running!)");
            longTask.cancel(true);
            System.out.println("  Cancelled the task. isCancelled() = " + longTask.isCancelled());
        }

        // --- cancel() demo ---
        System.out.println("\n--- 4c: cancel(mayInterruptIfRunning) — Cancel a running task ---");

        Future<String> cancellableTask = executor.submit(() -> {
            System.out.println("  [CancellableTask] Started, will run for 5 seconds...");
            try {
                for (int i = 1; i <= 5; i++) {
                    Thread.sleep(1000);
                    System.out.println("  [CancellableTask] Second " + i + "...");
                }
            } catch (InterruptedException e) {
                System.out.println("  [CancellableTask] Interrupted! Cleaning up and exiting.");
                Thread.currentThread().interrupt();
                return "interrupted";
            }
            return "finished";
        });

        Thread.sleep(2500);
        System.out.println("  Main thread: cancelling the task after 2.5 seconds...");
        boolean cancelled = cancellableTask.cancel(true);
        System.out.println("  cancel(true) returned: " + cancelled);
        System.out.println("  isCancelled() = " + cancellableTask.isCancelled());
        System.out.println("  isDone()      = " + cancellableTask.isDone());

        try {
            cancellableTask.get();
        } catch (CancellationException e) {
            System.out.println("  get() on cancelled task throws CancellationException — expected!");
        }

        // --- Combining isDone() with polling pattern ---
        System.out.println("\n--- 4d: Polling pattern with isDone() ---");

        Future<Integer> polledTask = executor.submit(() -> {
            Thread.sleep(1500);
            return 99;
        });

        System.out.println("  Polling until task completes (checking every 300ms):");
        while (!polledTask.isDone()) {
            System.out.println("    ... still waiting (isDone = false)");
            Thread.sleep(300);
        }
        System.out.println("  Task done! Result: " + polledTask.get());
        System.out.println("  Note: Polling wastes CPU. Prefer get() or CompletableFuture callbacks.");

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // ════════════════════════════════════════════
        //  SUMMARY
        // ════════════════════════════════════════════
        System.out.println("\n========================================");
        System.out.println("  SUMMARY: Runnable vs Callable");
        System.out.println("========================================");
        System.out.println("  ┌───────────────────┬─────────────────────┬──────────────────────┐");
        System.out.println("  │     Feature        │     Runnable        │     Callable<V>      │");
        System.out.println("  ├───────────────────┼─────────────────────┼──────────────────────┤");
        System.out.println("  │ Method            │ run()               │ call()               │");
        System.out.println("  │ Return value      │ void                │ V (generic)          │");
        System.out.println("  │ Checked exception │ No                  │ Yes                  │");
        System.out.println("  │ Error handling    │ UncaughtExcHandler  │ Future.get() throws  │");
        System.out.println("  │ Use with Thread   │ Yes                 │ Via FutureTask only   │");
        System.out.println("  │ Use with Executor │ execute() / submit()│ submit() only        │");
        System.out.println("  └───────────────────┴─────────────────────┴──────────────────────┘");
    }
}
