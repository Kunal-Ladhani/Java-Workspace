package com.scaler.concurrency.thread_creation;

import java.util.concurrent.*;

/**
 * Demonstrates ALL 6 ways to create and run threads in Java:
 *   1. Extending Thread class
 *   2. Implementing Runnable interface
 *   3. Lambda Runnable
 *   4. Callable + ExecutorService
 *   5. FutureTask + Thread
 *   6. ExecutorService with Thread Pool (showing thread reuse)
 *
 * Run main() and observe the console output.
 */
public class ThreadCreationDemo {

    // ──────────────────────────────────────────────
    //  Way 1: Extending Thread class
    // ──────────────────────────────────────────────
    static class MyThread extends Thread {
        private final String taskName;

        MyThread(String taskName) {
            this.taskName = taskName;
        }

        @Override
        public void run() {
            System.out.println("  [" + taskName + "] Running in thread: "
                    + Thread.currentThread().getName());
            System.out.println("  [" + taskName + "] Thread ID: "
                    + Thread.currentThread().getId());
        }
    }

    // ──────────────────────────────────────────────
    //  Way 2: Implementing Runnable interface
    // ──────────────────────────────────────────────
    static class MyRunnable implements Runnable {
        private final String taskName;

        MyRunnable(String taskName) {
            this.taskName = taskName;
        }

        @Override
        public void run() {
            System.out.println("  [" + taskName + "] Running in thread: "
                    + Thread.currentThread().getName());
            System.out.println("  [" + taskName + "] Runnable can be shared across multiple threads");
        }
    }

    public static void main(String[] args) throws Exception {

        // ════════════════════════════════════════════
        //  WAY 1: Extending Thread class
        // ════════════════════════════════════════════
        System.out.println("========================================");
        System.out.println("  WAY 1: Extending Thread Class");
        System.out.println("========================================");

        MyThread thread1 = new MyThread("ExtendThread");
        thread1.setName("MyThread-1");
        thread1.start();
        thread1.join();

        System.out.println("  Limitation: MyThread already extends Thread,");
        System.out.println("  so it CANNOT extend any other class.\n");

        Thread.sleep(300);

        // ════════════════════════════════════════════
        //  WAY 2: Implementing Runnable interface
        // ════════════════════════════════════════════
        System.out.println("========================================");
        System.out.println("  WAY 2: Implementing Runnable");
        System.out.println("========================================");

        MyRunnable myRunnable = new MyRunnable("ImplRunnable");

        Thread thread2 = new Thread(myRunnable, "RunnableThread-1");
        thread2.start();
        thread2.join();

        // Demonstrating reusability: same Runnable, different thread
        Thread thread2b = new Thread(myRunnable, "RunnableThread-2");
        thread2b.start();
        thread2b.join();

        System.out.println("  Advantage: Same Runnable instance used by two different threads.\n");

        Thread.sleep(300);

        // ════════════════════════════════════════════
        //  WAY 3: Lambda Runnable (Java 8+)
        // ════════════════════════════════════════════
        System.out.println("========================================");
        System.out.println("  WAY 3: Lambda Runnable (Java 8+)");
        System.out.println("========================================");

        Thread thread3 = new Thread(() -> {
            System.out.println("  [LambdaTask] Running in thread: "
                    + Thread.currentThread().getName());
            System.out.println("  [LambdaTask] Cleanest syntax for simple tasks!");
        }, "LambdaThread-1");

        thread3.start();
        thread3.join();

        // One-liner version
        Thread thread3b = new Thread(
                () -> System.out.println("  [OneLiner] Thread: "
                        + Thread.currentThread().getName()),
                "LambdaThread-2"
        );
        thread3b.start();
        thread3b.join();

        System.out.println("  Since Runnable is a @FunctionalInterface, lambdas work.\n");

        Thread.sleep(300);

        // ════════════════════════════════════════════
        //  WAY 4: Callable + ExecutorService
        // ════════════════════════════════════════════
        System.out.println("========================================");
        System.out.println("  WAY 4: Callable + ExecutorService");
        System.out.println("========================================");

        ExecutorService singleExecutor = Executors.newSingleThreadExecutor();

        Callable<Integer> computationTask = () -> {
            System.out.println("  [CallableTask] Computing in thread: "
                    + Thread.currentThread().getName());
            Thread.sleep(500);
            return 42;
        };

        System.out.println("  Submitting Callable to ExecutorService...");
        Future<Integer> future = singleExecutor.submit(computationTask);

        System.out.println("  future.isDone() = " + future.isDone() + "  (task still running)");

        Integer result = future.get();
        System.out.println("  future.get()    = " + result + "  (blocked until result was ready)");
        System.out.println("  future.isDone() = " + future.isDone() + "  (task completed)");

        singleExecutor.shutdown();
        singleExecutor.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("  Advantage: Callable returns a value and can throw checked exceptions.\n");

        Thread.sleep(300);

        // ════════════════════════════════════════════
        //  WAY 5: FutureTask + Thread
        // ════════════════════════════════════════════
        System.out.println("========================================");
        System.out.println("  WAY 5: FutureTask + Thread");
        System.out.println("========================================");

        Callable<String> stringCallable = () -> {
            System.out.println("  [FutureTaskCallable] Computing in thread: "
                    + Thread.currentThread().getName());
            Thread.sleep(300);
            return "Hello from FutureTask!";
        };

        FutureTask<String> futureTask = new FutureTask<>(stringCallable);
        System.out.println("  FutureTask wraps Callable and implements both Runnable + Future.");

        Thread thread5 = new Thread(futureTask, "FutureTaskThread-1");
        thread5.start();

        System.out.println("  Waiting for result...");
        String ftResult = futureTask.get();
        System.out.println("  futureTask.get() = \"" + ftResult + "\"");
        System.out.println("  FutureTask bridges Callable and Thread — no ExecutorService needed.\n");

        Thread.sleep(300);

        // ════════════════════════════════════════════
        //  WAY 6: ExecutorService with Thread Pool
        // ════════════════════════════════════════════
        System.out.println("========================================");
        System.out.println("  WAY 6: Thread Pool (ExecutorService)");
        System.out.println("========================================");

        int poolSize = 3;
        int taskCount = 5;
        System.out.println("  Pool size: " + poolSize + " threads, submitting " + taskCount + " tasks.");
        System.out.println("  Watch how thread names repeat — threads are REUSED!\n");

        ExecutorService poolExecutor = Executors.newFixedThreadPool(poolSize);

        CountDownLatch latch = new CountDownLatch(taskCount);

        for (int i = 1; i <= taskCount; i++) {
            final int taskId = i;
            poolExecutor.submit(() -> {
                String threadName = Thread.currentThread().getName();
                System.out.println("  [Task-" + taskId + "] Running on: " + threadName);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("  [Task-" + taskId + "] Completed on: " + threadName);
                latch.countDown();
            });
        }

        latch.await();

        poolExecutor.shutdown();
        poolExecutor.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("\n  All 5 tasks done, but only 3 threads were used (pool-1-thread-1/2/3).");
        System.out.println("  In production, ALWAYS prefer thread pools over manual thread creation.");

        // ════════════════════════════════════════════
        //  SUMMARY
        // ════════════════════════════════════════════
        System.out.println("\n========================================");
        System.out.println("  SUMMARY");
        System.out.println("========================================");
        System.out.println("  Way 1: extends Thread        → Simple but inflexible (single inheritance)");
        System.out.println("  Way 2: implements Runnable    → Preferred for tasks (composition over inheritance)");
        System.out.println("  Way 3: Lambda Runnable        → Cleanest syntax (Java 8+)");
        System.out.println("  Way 4: Callable + Executor    → When you need a return value");
        System.out.println("  Way 5: FutureTask + Thread    → Callable without ExecutorService");
        System.out.println("  Way 6: Thread Pool            → ALWAYS use in production code");
    }
}
