package com.scaler.concurrency.reenterant_lock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Demonstrates Fair vs Non-Fair ReentrantLock behavior:
 *   1. Side-by-side acquisition order comparison (10 threads each)
 *   2. Throughput comparison (fair vs non-fair under contention)
 *   3. The tryLock() fairness caveat
 *
 * Run main() and observe the console output.
 */
public class FairLockDemo {

    private static final List<String> acquisitionOrder = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║            FAIR LOCK DEMO — ReentrantLock           ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");

        System.out.println("\n========================================");
        System.out.println("  DEMO 1: Acquisition Order — Non-Fair");
        System.out.println("========================================");
        demoAcquisitionOrder(new ReentrantLock(false), "Non-Fair");

        Thread.sleep(1000);

        System.out.println("\n========================================");
        System.out.println("  DEMO 1: Acquisition Order — Fair");
        System.out.println("========================================");
        demoAcquisitionOrder(new ReentrantLock(true), "Fair");

        Thread.sleep(1000);

        System.out.println("\n========================================");
        System.out.println("  DEMO 2: Throughput Comparison");
        System.out.println("========================================");
        demoThroughput();

        Thread.sleep(500);

        System.out.println("\n========================================");
        System.out.println("  DEMO 3: tryLock() Fairness Caveat");
        System.out.println("========================================");
        demoTryLockFairnessCaveat();
    }

    // ──────────────────────────────────────────────────────────────
    //  DEMO 1: Acquisition Order
    //  10 threads compete for the lock. With a fair lock, they
    //  acquire in arrival order. With non-fair, order is random.
    // ──────────────────────────────────────────────────────────────
    private static void demoAcquisitionOrder(ReentrantLock lock, String label) throws InterruptedException {
        acquisitionOrder.clear();
        System.out.println("Lock fairness = " + lock.isFair());

        lock.lock();

        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            final int id = i + 1;
            threads[i] = new Thread(() -> {
                lock.lock();
                try {
                    String entry = "Thread-" + String.format("%02d", id);
                    synchronized (acquisitionOrder) {
                        acquisitionOrder.add(entry);
                    }
                    System.out.println("  " + entry + " acquired lock");
                } finally {
                    lock.unlock();
                }
            }, "Worker-" + id);
            threads[i].start();
            Thread.sleep(30);
        }

        Thread.sleep(200);
        System.out.println("\nReleasing lock — all 10 threads will now compete:\n");
        lock.unlock();

        for (Thread t : threads) {
            t.join();
        }

        System.out.println("\n" + label + " acquisition order: " + acquisitionOrder);

        if (lock.isFair()) {
            System.out.println("→ With FAIR lock: threads acquired in exact arrival order (1-10).");
        } else {
            System.out.println("→ With NON-FAIR lock: order may vary due to barging.");
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  DEMO 2: Throughput Comparison
    //  Multiple threads do many lock/unlock cycles.
    //  Fair lock forces context switches, reducing throughput.
    // ──────────────────────────────────────────────────────────────
    private static void demoThroughput() throws InterruptedException {
        int numThreads = 4;
        int operationsPerThread = 50_000;

        long nonFairTime = measureThroughput(new ReentrantLock(false), numThreads, operationsPerThread);
        long fairTime = measureThroughput(new ReentrantLock(true), numThreads, operationsPerThread);

        System.out.println("\nResults (" + numThreads + " threads, " + operationsPerThread + " ops/thread):");
        System.out.println("  Non-Fair lock: " + nonFairTime + " ms");
        System.out.println("  Fair lock:     " + fairTime + " ms");

        if (fairTime > 0 && nonFairTime > 0) {
            double ratio = (double) fairTime / nonFairTime;
            System.out.printf("  Fair is %.1fx slower than non-fair%n", ratio);
        }

        System.out.println("\n→ Non-fair lock avoids context switches by letting running threads barge in.");
        System.out.println("→ Fair lock forces FIFO ordering, causing a context switch per acquisition.");
    }

    private static long measureThroughput(ReentrantLock lock, int numThreads, int opsPerThread)
            throws InterruptedException {
        final long[] counter = {0};

        Thread[] threads = new Thread[numThreads];
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < opsPerThread; j++) {
                    lock.lock();
                    try {
                        counter[0]++;
                    } finally {
                        lock.unlock();
                    }
                }
            });
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("  [" + (lock.isFair() ? "Fair" : "Non-Fair") + "] "
                + "Total ops: " + counter[0] + " in " + elapsed + " ms");
        return elapsed;
    }

    // ──────────────────────────────────────────────────────────────
    //  DEMO 3: tryLock() Fairness Caveat
    //  Zero-arg tryLock() does NOT respect fairness, even on a
    //  fair lock. Use tryLock(0, SECONDS) for fair behavior.
    // ──────────────────────────────────────────────────────────────
    private static void demoTryLockFairnessCaveat() throws InterruptedException {
        ReentrantLock fairLock = new ReentrantLock(true);

        fairLock.lock();

        Thread waiter = new Thread(() -> {
            System.out.println("  [Waiter] Waiting for fair lock...");
            fairLock.lock();
            try {
                System.out.println("  [Waiter] Finally acquired the lock!");
            } finally {
                fairLock.unlock();
            }
        }, "Waiter");
        waiter.start();
        Thread.sleep(100);

        Thread barger = new Thread(() -> {
            boolean acquired = fairLock.tryLock();
            if (acquired) {
                try {
                    System.out.println("  [Barger-tryLock] BARGING SUCCESS — stole lock despite fair mode!");
                } finally {
                    fairLock.unlock();
                }
            } else {
                System.out.println("  [Barger-tryLock] Could not acquire (lock held by main).");
            }
        }, "Barger-tryLock");

        Thread fairTrier = new Thread(() -> {
            try {
                boolean acquired = fairLock.tryLock(0, TimeUnit.SECONDS);
                if (acquired) {
                    try {
                        System.out.println("  [FairTrier] Acquired with tryLock(0, SECONDS).");
                    } finally {
                        fairLock.unlock();
                    }
                } else {
                    System.out.println("  [FairTrier] tryLock(0, SECONDS) respected fairness — did NOT barge.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "FairTrier");

        System.out.println("\nMain holds lock. Waiter is queued. Now testing barging:\n");

        fairLock.unlock();
        Thread.sleep(50);

        fairLock.lock();
        Thread.sleep(100);

        barger.start();
        fairTrier.start();
        Thread.sleep(100);

        System.out.println("\n  [Main] Releasing lock to let remaining threads proceed...\n");
        fairLock.unlock();

        waiter.join(3000);
        barger.join(3000);
        fairTrier.join(3000);

        System.out.println("\n→ Key Takeaway:");
        System.out.println("  tryLock()             → does NOT respect fairness (barges)");
        System.out.println("  tryLock(0, SECONDS)   → DOES respect fairness (checks queue)");
    }
}
