package com.scaler.concurrency.race_condition;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstrates race conditions and their fixes with clear, runnable examples.
 */
public class RaceConditionDemo {

    // ──────────────────────────────────────────────────────────────
    // Section 1: Broken counter (Read-Modify-Write race condition)
    // ──────────────────────────────────────────────────────────────

    private static int unsafeCounter = 0;

    private static void demoBrokenCounter() throws InterruptedException {
        System.out.println("==========================================================");
        System.out.println("  DEMO 1: Broken Counter — Race Condition in Action");
        System.out.println("==========================================================");
        System.out.println("Two threads each increment a shared counter 100,000 times.");
        System.out.println("Expected final value: 200,000\n");

        int iterations = 100_000;

        for (int trial = 1; trial <= 5; trial++) {
            unsafeCounter = 0;

            Thread t1 = new Thread(() -> {
                for (int i = 0; i < iterations; i++) {
                    unsafeCounter++;
                }
            });
            Thread t2 = new Thread(() -> {
                for (int i = 0; i < iterations; i++) {
                    unsafeCounter++;
                }
            });

            t1.start();
            t2.start();
            t1.join();
            t2.join();

            int expected = 2 * iterations;
            int lost = expected - unsafeCounter;
            System.out.printf("  Trial %d: counter = %,d  (lost %,d increments)%n",
                    trial, unsafeCounter, lost);
        }
        System.out.println("\n  → The counter is almost never 200,000.");
        System.out.println("    This is a classic read-modify-write race condition.\n");
    }

    // ──────────────────────────────────────────────────────────────
    // Section 2: Fixed with synchronized
    // ──────────────────────────────────────────────────────────────

    private static int syncCounter = 0;
    private static final Object lock = new Object();

    private static void demoFixedWithSynchronized() throws InterruptedException {
        System.out.println("==========================================================");
        System.out.println("  DEMO 2: Counter Fixed with synchronized");
        System.out.println("==========================================================");

        int iterations = 100_000;

        for (int trial = 1; trial <= 3; trial++) {
            syncCounter = 0;

            Thread t1 = new Thread(() -> {
                for (int i = 0; i < iterations; i++) {
                    synchronized (lock) {
                        syncCounter++;
                    }
                }
            });
            Thread t2 = new Thread(() -> {
                for (int i = 0; i < iterations; i++) {
                    synchronized (lock) {
                        syncCounter++;
                    }
                }
            });

            t1.start();
            t2.start();
            t1.join();
            t2.join();

            System.out.printf("  Trial %d: counter = %,d  (expected: %,d)%n",
                    trial, syncCounter, 2 * iterations);
        }
        System.out.println("\n  → Always correct. synchronized ensures mutual exclusion.\n");
    }

    // ──────────────────────────────────────────────────────────────
    // Section 3: Fixed with AtomicInteger
    // ──────────────────────────────────────────────────────────────

    private static final AtomicInteger atomicCounter = new AtomicInteger(0);

    private static void demoFixedWithAtomic() throws InterruptedException {
        System.out.println("==========================================================");
        System.out.println("  DEMO 3: Counter Fixed with AtomicInteger");
        System.out.println("==========================================================");

        int iterations = 100_000;

        for (int trial = 1; trial <= 3; trial++) {
            atomicCounter.set(0);

            Thread t1 = new Thread(() -> {
                for (int i = 0; i < iterations; i++) {
                    atomicCounter.incrementAndGet();
                }
            });
            Thread t2 = new Thread(() -> {
                for (int i = 0; i < iterations; i++) {
                    atomicCounter.incrementAndGet();
                }
            });

            t1.start();
            t2.start();
            t1.join();
            t2.join();

            System.out.printf("  Trial %d: counter = %,d  (expected: %,d)%n",
                    trial, atomicCounter.get(), 2 * iterations);
        }
        System.out.println("\n  → Always correct. AtomicInteger uses CAS — lock-free & fast.\n");
    }

    // ──────────────────────────────────────────────────────────────
    // Section 4: Check-Then-Act — Broken Lazy Singleton
    // ──────────────────────────────────────────────────────────────

    static class BrokenSingleton {
        private static BrokenSingleton instance;
        private static int instanceCount = 0;

        private BrokenSingleton() {
            instanceCount++;
        }

        public static BrokenSingleton getInstance() {
            if (instance == null) {
                try { Thread.sleep(0, 1); } catch (InterruptedException ignored) {}
                instance = new BrokenSingleton();
            }
            return instance;
        }

        public static int getInstanceCount() { return instanceCount; }
        public static void reset() { instance = null; instanceCount = 0; }
    }

    static class FixedSingleton {
        private static volatile FixedSingleton instance;
        private static int instanceCount = 0;

        private FixedSingleton() {
            instanceCount++;
        }

        public static FixedSingleton getInstance() {
            if (instance == null) {
                synchronized (FixedSingleton.class) {
                    if (instance == null) {
                        instance = new FixedSingleton();
                    }
                }
            }
            return instance;
        }

        public static int getInstanceCount() { return instanceCount; }
        public static void reset() { instance = null; instanceCount = 0; }
    }

    private static void demoCheckThenAct() throws InterruptedException {
        System.out.println("==========================================================");
        System.out.println("  DEMO 4: Check-Then-Act — Broken vs Fixed Singleton");
        System.out.println("==========================================================");

        System.out.println("\n--- Broken Singleton (no synchronization) ---");
        int brokenRaces = 0;
        int trials = 20;

        for (int trial = 0; trial < trials; trial++) {
            BrokenSingleton.reset();
            int threadCount = 10;
            Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(BrokenSingleton::getInstance);
            }
            for (Thread t : threads) t.start();
            for (Thread t : threads) t.join();

            int count = BrokenSingleton.getInstanceCount();
            if (count > 1) {
                brokenRaces++;
            }
        }
        System.out.println("  Races detected in " + brokenRaces + "/" + trials + " trials.");
        System.out.println("  (Multiple instances created — singleton invariant violated!)\n");

        System.out.println("--- Fixed Singleton (double-checked locking + volatile) ---");
        int fixedRaces = 0;

        for (int trial = 0; trial < trials; trial++) {
            FixedSingleton.reset();
            int threadCount = 10;
            Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(FixedSingleton::getInstance);
            }
            for (Thread t : threads) t.start();
            for (Thread t : threads) t.join();

            int count = FixedSingleton.getInstanceCount();
            if (count > 1) {
                fixedRaces++;
            }
        }
        System.out.println("  Races detected in " + fixedRaces + "/" + trials + " trials.");
        System.out.println("  (Always exactly one instance — double-checked locking works.)\n");
    }

    // ──────────────────────────────────────────────────────────────
    // Section 5: Side-by-side comparison
    // ──────────────────────────────────────────────────────────────

    private static void demoSideBySide() throws InterruptedException {
        System.out.println("==========================================================");
        System.out.println("  DEMO 5: Performance Comparison — Sync vs Atomic vs Unsafe");
        System.out.println("==========================================================");

        int iterations = 1_000_000;

        // Unsafe (just for timing — result will be wrong)
        unsafeCounter = 0;
        long start = System.nanoTime();
        Thread t1 = new Thread(() -> { for (int i = 0; i < iterations; i++) unsafeCounter++; });
        Thread t2 = new Thread(() -> { for (int i = 0; i < iterations; i++) unsafeCounter++; });
        t1.start(); t2.start(); t1.join(); t2.join();
        long unsafeTime = System.nanoTime() - start;

        // Synchronized
        syncCounter = 0;
        start = System.nanoTime();
        t1 = new Thread(() -> { for (int i = 0; i < iterations; i++) synchronized (lock) { syncCounter++; } });
        t2 = new Thread(() -> { for (int i = 0; i < iterations; i++) synchronized (lock) { syncCounter++; } });
        t1.start(); t2.start(); t1.join(); t2.join();
        long syncTime = System.nanoTime() - start;

        // Atomic
        atomicCounter.set(0);
        start = System.nanoTime();
        t1 = new Thread(() -> { for (int i = 0; i < iterations; i++) atomicCounter.incrementAndGet(); });
        t2 = new Thread(() -> { for (int i = 0; i < iterations; i++) atomicCounter.incrementAndGet(); });
        t1.start(); t2.start(); t1.join(); t2.join();
        long atomicTime = System.nanoTime() - start;

        System.out.printf("  Unsafe (WRONG result):  %,d  in %,d ms%n",
                unsafeCounter, unsafeTime / 1_000_000);
        System.out.printf("  Synchronized (correct): %,d  in %,d ms%n",
                syncCounter, syncTime / 1_000_000);
        System.out.printf("  AtomicInteger (correct): %,d  in %,d ms%n",
                atomicCounter.get(), atomicTime / 1_000_000);
        System.out.println("\n  → Atomic is typically fastest among correct solutions.");
        System.out.println("    Unsafe is fast but WRONG — speed without correctness is meaningless.\n");
    }

    // ──────────────────────────────────────────────────────────────
    // Main
    // ──────────────────────────────────────────────────────────────

    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║           RACE CONDITION DEMO — JAVA                    ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");

        demoBrokenCounter();
        demoFixedWithSynchronized();
        demoFixedWithAtomic();
        demoCheckThenAct();
        demoSideBySide();

        System.out.println("==========================================================");
        System.out.println("  All demos completed.");
        System.out.println("==========================================================");
    }
}
