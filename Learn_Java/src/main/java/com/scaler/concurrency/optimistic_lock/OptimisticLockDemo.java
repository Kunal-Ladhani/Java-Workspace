package com.scaler.concurrency.optimistic_lock;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * Demonstrates Optimistic Locking patterns:
 *   1. CAS-based counter using AtomicInteger
 *   2. Custom optimistic lock with version number (DB-style versioning)
 *   3. ABA problem demonstration
 *   4. ABA problem fix using AtomicStampedReference
 *   5. Throughput comparison: optimistic (AtomicInteger) vs pessimistic (synchronized)
 *
 * Run main() and observe the console output.
 */
public class OptimisticLockDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║        OPTIMISTIC LOCK DEMO — CAS & Versioning          ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");

        System.out.println("\n========================================");
        System.out.println("  DEMO 1: CAS-Based Counter");
        System.out.println("========================================");
        demoCASCounter();

        Thread.sleep(500);

        System.out.println("\n========================================");
        System.out.println("  DEMO 2: Custom Versioned Record");
        System.out.println("  (DB-Style Optimistic Locking)");
        System.out.println("========================================");
        demoVersionedRecord();

        Thread.sleep(500);

        System.out.println("\n========================================");
        System.out.println("  DEMO 3: ABA Problem Demonstration");
        System.out.println("========================================");
        demoABAProblem();

        Thread.sleep(500);

        System.out.println("\n========================================");
        System.out.println("  DEMO 4: ABA Fix with");
        System.out.println("  AtomicStampedReference");
        System.out.println("========================================");
        demoABAFix();

        Thread.sleep(500);

        System.out.println("\n========================================");
        System.out.println("  DEMO 5: Throughput Comparison");
        System.out.println("  Optimistic vs Pessimistic");
        System.out.println("========================================");
        demoThroughputComparison();
    }

    // ──────────────────────────────────────────────────────────────
    //  DEMO 1: CAS-Based Counter
    //  Multiple threads increment a counter using CAS retry loop.
    //  No locks needed — pure optimistic concurrency.
    // ──────────────────────────────────────────────────────────────
    private static void demoCASCounter() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger totalRetries = new AtomicInteger(0);
        int numThreads = 10;
        int incrementsPerThread = 10_000;

        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                int localRetries = 0;
                for (int j = 0; j < incrementsPerThread; j++) {
                    int oldVal, newVal;
                    boolean success;
                    do {
                        oldVal = counter.get();
                        newVal = oldVal + 1;
                        success = counter.compareAndSet(oldVal, newVal);
                        if (!success) {
                            localRetries++;
                        }
                    } while (!success);
                }
                totalRetries.addAndGet(localRetries);
            });
        }

        long start = System.currentTimeMillis();
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        long elapsed = System.currentTimeMillis() - start;

        int expected = numThreads * incrementsPerThread;
        System.out.println("  Threads:         " + numThreads);
        System.out.println("  Increments each: " + incrementsPerThread);
        System.out.println("  Expected total:  " + expected);
        System.out.println("  Actual total:    " + counter.get());
        System.out.println("  Time:            " + elapsed + " ms");
        System.out.println("  Correct?         " + (counter.get() == expected ? "YES" : "NO"));
        System.out.println("\n  Now using incrementAndGet() (built-in CAS loop):");

        AtomicInteger counter2 = new AtomicInteger(0);
        Thread[] threads2 = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads2[i] = new Thread(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    counter2.incrementAndGet();
                }
            });
        }
        start = System.currentTimeMillis();
        for (Thread t : threads2) t.start();
        for (Thread t : threads2) t.join();
        elapsed = System.currentTimeMillis() - start;

        System.out.println("  Result:          " + counter2.get() + " in " + elapsed + " ms");
        System.out.println("\n→ CAS guarantees correctness without any lock.");
    }

    // ──────────────────────────────────────────────────────────────
    //  DEMO 2: Custom Versioned Record (DB-Style)
    //  Simulates optimistic locking like a database @Version column.
    // ──────────────────────────────────────────────────────────────
    static class VersionedRecord {
        private volatile int value;
        private volatile int version;

        VersionedRecord(int initialValue) {
            this.value = initialValue;
            this.version = 0;
        }

        public synchronized int[] read() {
            return new int[]{value, version};
        }

        public synchronized boolean writeIfVersionMatches(int newValue, int expectedVersion) {
            if (this.version == expectedVersion) {
                this.value = newValue;
                this.version++;
                return true;
            }
            return false;
        }

        public synchronized int getValue() { return value; }
        public synchronized int getVersion() { return version; }
    }

    private static void demoVersionedRecord() throws InterruptedException {
        VersionedRecord record = new VersionedRecord(100);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        int numThreads = 10;

        System.out.println("  Initial value: " + record.getValue() + ", version: " + record.getVersion());
        System.out.println("  " + numThreads + " threads each try to set value = threadId * 10\n");

        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i + 1;
            threads[i] = new Thread(() -> {
                int maxRetries = 5;
                for (int attempt = 1; attempt <= maxRetries; attempt++) {
                    int[] snapshot = record.read();
                    int currentVersion = snapshot[1];

                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }

                    int newValue = threadId * 10;
                    boolean success = record.writeIfVersionMatches(newValue, currentVersion);
                    if (success) {
                        System.out.println("  Thread-" + threadId + " SUCCESS on attempt " + attempt
                                + " (set value=" + newValue + ", version=" + (currentVersion + 1) + ")");
                        successCount.incrementAndGet();
                        return;
                    } else {
                        System.out.println("  Thread-" + threadId + " CONFLICT on attempt " + attempt
                                + " (expected version=" + currentVersion + ", actual changed) → retrying");
                        failCount.incrementAndGet();
                    }
                }
                System.out.println("  Thread-" + threadId + " gave up after " + 5 + " retries.");
            }, "Thread-" + threadId);
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        System.out.println("\n  Final value:     " + record.getValue());
        System.out.println("  Final version:   " + record.getVersion());
        System.out.println("  Total successes: " + successCount.get());
        System.out.println("  Total conflicts: " + failCount.get());
        System.out.println("\n→ Only one thread can commit per version. Others detect the conflict and retry.");
    }

    // ──────────────────────────────────────────────────────────────
    //  DEMO 3: ABA Problem
    //  Shows how CAS can be fooled when value changes A → B → A.
    // ──────────────────────────────────────────────────────────────
    private static void demoABAProblem() throws InterruptedException {
        AtomicInteger sharedValue = new AtomicInteger(10);

        System.out.println("  Initial value: " + sharedValue.get());

        Thread victim = new Thread(() -> {
            int expected = sharedValue.get();
            System.out.println("  [Victim] Read value = " + expected + ". Will try CAS(" + expected + " → 20) later...");

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            boolean success = sharedValue.compareAndSet(expected, 20);
            System.out.println("  [Victim] CAS(" + expected + " → 20) = " + success);
            if (success) {
                System.out.println("  [Victim] CAS succeeded, but value was changed A→B→A in between!");
                System.out.println("  [Victim] This is the ABA problem — CAS didn't detect intermediate changes.");
            }
        }, "Victim");

        Thread meddler = new Thread(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            int original = sharedValue.get();
            System.out.println("  [Meddler] Changing value: " + original + " → 99");
            sharedValue.set(99);

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            System.out.println("  [Meddler] Changing value back: 99 → " + original + " (ABA!)");
            sharedValue.set(original);
        }, "Meddler");

        victim.start();
        meddler.start();
        victim.join();
        meddler.join();

        System.out.println("\n  Final value: " + sharedValue.get());
        System.out.println("\n→ CAS only checks the CURRENT value, not whether it CHANGED in between.");
        System.out.println("→ For simple counters this is harmless. For pointers/linked structures, it's dangerous.");
    }

    // ──────────────────────────────────────────────────────────────
    //  DEMO 4: ABA Fix with AtomicStampedReference
    //  Stamp (version) changes even when value goes A → B → A.
    // ──────────────────────────────────────────────────────────────
    private static void demoABAFix() throws InterruptedException {
        AtomicStampedReference<Integer> stampedRef = new AtomicStampedReference<>(10, 0);

        System.out.println("  Initial value: " + stampedRef.getReference() + ", stamp: " + stampedRef.getStamp());

        Thread victim = new Thread(() -> {
            int[] stampHolder = new int[1];
            Integer expected = stampedRef.get(stampHolder);
            int expectedStamp = stampHolder[0];
            System.out.println("  [Victim] Read value=" + expected + ", stamp=" + expectedStamp
                    + ". Will try CAS later...");

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            boolean success = stampedRef.compareAndSet(expected, 20, expectedStamp, expectedStamp + 1);
            System.out.println("  [Victim] CAS(value=" + expected + "→20, stamp=" + expectedStamp
                    + "→" + (expectedStamp + 1) + ") = " + success);
            if (!success) {
                int[] currentStampHolder = new int[1];
                Integer currentVal = stampedRef.get(currentStampHolder);
                System.out.println("  [Victim] CAS FAILED! Current value=" + currentVal
                        + ", stamp=" + currentStampHolder[0] + " (stamp changed due to ABA)");
                System.out.println("  [Victim] ABA DETECTED and PREVENTED by AtomicStampedReference!");
            }
        }, "Victim");

        Thread meddler = new Thread(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            int[] stampHolder = new int[1];
            Integer val = stampedRef.get(stampHolder);
            int stamp = stampHolder[0];
            System.out.println("  [Meddler] Changing value: " + val + " → 99 (stamp " + stamp + " → " + (stamp + 1) + ")");
            stampedRef.compareAndSet(val, 99, stamp, stamp + 1);

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            val = stampedRef.get(stampHolder);
            stamp = stampHolder[0];
            System.out.println("  [Meddler] Changing back: " + val + " → 10 (stamp " + stamp + " → " + (stamp + 1) + ")");
            stampedRef.compareAndSet(val, 10, stamp, stamp + 1);
        }, "Meddler");

        victim.start();
        meddler.start();
        victim.join();
        meddler.join();

        int[] finalStamp = new int[1];
        Integer finalVal = stampedRef.get(finalStamp);
        System.out.println("\n  Final value: " + finalVal + ", stamp: " + finalStamp[0]);
        System.out.println("\n→ AtomicStampedReference tracks a version stamp alongside the value.");
        System.out.println("→ Even though value went 10→99→10, stamp went 0→1→2, so CAS detected the change.");
    }

    // ──────────────────────────────────────────────────────────────
    //  DEMO 5: Throughput Comparison
    //  Optimistic (AtomicInteger) vs Pessimistic (synchronized)
    //  under low contention.
    // ──────────────────────────────────────────────────────────────
    private static void demoThroughputComparison() throws InterruptedException {
        int numThreads = 4;
        int opsPerThread = 1_000_000;
        int totalOps = numThreads * opsPerThread;

        // Optimistic: AtomicInteger
        AtomicInteger atomicCounter = new AtomicInteger(0);
        long optimisticTime = benchmark(numThreads, opsPerThread, () -> atomicCounter.incrementAndGet());

        // Pessimistic: synchronized
        final int[] syncCounter = {0};
        final Object syncLock = new Object();
        long pessimisticTime = benchmark(numThreads, opsPerThread, () -> {
            synchronized (syncLock) {
                syncCounter[0]++;
            }
        });

        System.out.println("\n  Configuration: " + numThreads + " threads × "
                + opsPerThread + " ops = " + totalOps + " total operations\n");
        System.out.println("  ┌──────────────────────┬────────────┬──────────────┐");
        System.out.println("  │ Approach             │ Time (ms)  │ Result       │");
        System.out.println("  ├──────────────────────┼────────────┼──────────────┤");
        System.out.printf("  │ Optimistic (Atomic)  │ %10d │ %,12d │%n", optimisticTime, atomicCounter.get());
        System.out.printf("  │ Pessimistic (synced) │ %10d │ %,12d │%n", pessimisticTime, syncCounter[0]);
        System.out.println("  └──────────────────────┴────────────┴──────────────┘");

        if (pessimisticTime > 0) {
            double ratio = (double) pessimisticTime / optimisticTime;
            System.out.printf("\n  Pessimistic is %.1fx %s than optimistic.%n",
                    ratio > 1 ? ratio : 1.0 / ratio,
                    ratio > 1 ? "slower" : "faster");
        }

        System.out.println("\n→ Under low-moderate contention, optimistic (CAS) typically wins.");
        System.out.println("→ CAS avoids the overhead of monitor enter/exit (context switching).");
        System.out.println("→ Both approaches produce the correct result (" + totalOps + ").");
    }

    @FunctionalInterface
    interface Action {
        void run();
    }

    private static long benchmark(int numThreads, int opsPerThread, Action action) throws InterruptedException {
        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < opsPerThread; j++) {
                    action.run();
                }
            });
        }

        long start = System.currentTimeMillis();
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        return System.currentTimeMillis() - start;
    }
}
