package com.scaler.concurrency.spin_lock;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class SpinLockDemo {

    // ==================== TAS Spin Lock Implementation ====================

    static class TASSpinLock {
        private final AtomicBoolean locked = new AtomicBoolean(false);

        public void lock() {
            while (!locked.compareAndSet(false, true)) {
                Thread.onSpinWait();
            }
        }

        public void unlock() {
            locked.set(false);
        }
    }

    // ==================== TTAS Spin Lock Implementation ====================

    static class TTASSpinLock {
        private final AtomicBoolean locked = new AtomicBoolean(false);

        public void lock() {
            while (true) {
                while (locked.get()) {
                    Thread.onSpinWait();
                }
                if (locked.compareAndSet(false, true)) {
                    return;
                }
            }
        }

        public void unlock() {
            locked.set(false);
        }
    }

    // ==================== DEMO 1: TAS Spin Lock in Action ====================

    private static volatile int tasCounter = 0;

    private static void demoTASSpinLock() throws InterruptedException {
        System.out.println("=".repeat(70));
        System.out.println("DEMO 1: TAS (Test-And-Set) Spin Lock");
        System.out.println("  4 threads incrementing a shared counter 250,000 times each");
        System.out.println("=".repeat(70));

        TASSpinLock spinLock = new TASSpinLock();
        tasCounter = 0;
        int numThreads = 4;
        int incrementsPerThread = 250_000;

        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    spinLock.lock();
                    try {
                        tasCounter++;
                    } finally {
                        spinLock.unlock();
                    }
                }
            });
        }

        long start = System.nanoTime();
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        long elapsed = System.nanoTime() - start;

        int expected = numThreads * incrementsPerThread;
        System.out.printf("  Expected: %,d%n", expected);
        System.out.printf("  Actual:   %,d%n", tasCounter);
        System.out.printf("  Correct:  %s%n", tasCounter == expected ? "YES" : "NO — RACE CONDITION!");
        System.out.printf("  Time:     %.2f ms%n%n", elapsed / 1_000_000.0);
    }

    // ==================== DEMO 2: TTAS Spin Lock in Action ====================

    private static volatile int ttasCounter = 0;

    private static void demoTTASSpinLock() throws InterruptedException {
        System.out.println("=".repeat(70));
        System.out.println("DEMO 2: TTAS (Test-And-Test-And-Set) Spin Lock");
        System.out.println("  4 threads incrementing a shared counter 250,000 times each");
        System.out.println("  TTAS reads first (local cache), then CAS only if lock looks free");
        System.out.println("=".repeat(70));

        TTASSpinLock spinLock = new TTASSpinLock();
        ttasCounter = 0;
        int numThreads = 4;
        int incrementsPerThread = 250_000;

        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    spinLock.lock();
                    try {
                        ttasCounter++;
                    } finally {
                        spinLock.unlock();
                    }
                }
            });
        }

        long start = System.nanoTime();
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        long elapsed = System.nanoTime() - start;

        int expected = numThreads * incrementsPerThread;
        System.out.printf("  Expected: %,d%n", expected);
        System.out.printf("  Actual:   %,d%n", ttasCounter);
        System.out.printf("  Correct:  %s%n", ttasCounter == expected ? "YES" : "NO — RACE CONDITION!");
        System.out.printf("  Time:     %.2f ms%n%n", elapsed / 1_000_000.0);
    }

    // ==================== DEMO 3: Spin Lock Correctness with Detailed Output ====================

    private static void demoSpinLockDetailed() throws InterruptedException {
        System.out.println("=".repeat(70));
        System.out.println("DEMO 3: Spin Lock in Action — Detailed Thread Output");
        System.out.println("  3 threads using a TAS spin lock to protect a critical section");
        System.out.println("=".repeat(70));

        TASSpinLock spinLock = new TASSpinLock();
        Thread[] threads = new Thread[3];

        for (int i = 0; i < 3; i++) {
            final int id = i + 1;
            threads[i] = new Thread(() -> {
                for (int round = 1; round <= 3; round++) {
                    System.out.printf("  [Thread-%d] Attempting to acquire spin lock (round %d)...%n", id, round);
                    spinLock.lock();
                    try {
                        System.out.printf("  [Thread-%d] ✓ ACQUIRED spin lock (round %d) — in critical section%n", id, round);
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    } finally {
                        spinLock.unlock();
                        System.out.printf("  [Thread-%d] Released spin lock (round %d)%n", id, round);
                    }
                }
            }, "Thread-" + id);
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        System.out.println("\n  Spin lock correctly ensured mutual exclusion across all rounds.\n");
    }

    // ==================== DEMO 4: Performance Comparison ====================

    private static void demoPerformanceComparison() throws InterruptedException {
        System.out.println("=".repeat(70));
        System.out.println("DEMO 4: Performance Comparison — SpinLock vs ReentrantLock");
        System.out.println("  Testing with very short critical section (single increment)");
        System.out.println("  4 threads, 500,000 increments each");
        System.out.println("=".repeat(70));

        int numThreads = 4;
        int incrementsPerThread = 500_000;

        // --- TAS Spin Lock ---
        TASSpinLock tasLock = new TASSpinLock();
        long tasTime = benchmark("TAS SpinLock", numThreads, incrementsPerThread, () -> {
            tasLock.lock();
            try {
                tasCounter++;
            } finally {
                tasLock.unlock();
            }
        });

        // --- TTAS Spin Lock ---
        TTASSpinLock ttasLock = new TTASSpinLock();
        long ttasTime = benchmark("TTAS SpinLock", numThreads, incrementsPerThread, () -> {
            ttasLock.lock();
            try {
                ttasCounter++;
            } finally {
                ttasLock.unlock();
            }
        });

        // --- ReentrantLock ---
        ReentrantLock reentrantLock = new ReentrantLock();
        long rlTime = benchmark("ReentrantLock", numThreads, incrementsPerThread, () -> {
            reentrantLock.lock();
            try {
                tasCounter++;
            } finally {
                reentrantLock.unlock();
            }
        });

        // --- synchronized ---
        Object syncObj = new Object();
        long syncTime = benchmark("synchronized", numThreads, incrementsPerThread, () -> {
            synchronized (syncObj) {
                tasCounter++;
            }
        });

        System.out.println("  Summary:");
        System.out.println("  ┌─────────────────────┬──────────────┐");
        System.out.println("  │ Lock Type            │ Time (ms)    │");
        System.out.println("  ├─────────────────────┼──────────────┤");
        System.out.printf("  │ TAS SpinLock        │ %10.2f   │%n", tasTime / 1_000_000.0);
        System.out.printf("  │ TTAS SpinLock       │ %10.2f   │%n", ttasTime / 1_000_000.0);
        System.out.printf("  │ ReentrantLock       │ %10.2f   │%n", rlTime / 1_000_000.0);
        System.out.printf("  │ synchronized        │ %10.2f   │%n", syncTime / 1_000_000.0);
        System.out.println("  └─────────────────────┴──────────────┘");
        System.out.println();
        System.out.println("  NOTE: Results vary by CPU, contention, and JVM warmup.");
        System.out.println("  For very short critical sections, spin locks can be competitive.");
        System.out.println("  For real applications, prefer synchronized or ReentrantLock.");
        System.out.println();
    }

    private static long benchmark(String name, int numThreads, int incrementsPerThread,
                                  Runnable criticalSection) throws InterruptedException {
        tasCounter = 0;
        ttasCounter = 0;
        Thread[] threads = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    criticalSection.run();
                }
            });
        }

        long start = System.nanoTime();
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        long elapsed = System.nanoTime() - start;

        System.out.printf("  %-20s → %10.2f ms%n", name, elapsed / 1_000_000.0);
        return elapsed;
    }

    // ==================== DEMO 5: No Lock vs Spin Lock ====================

    private static volatile int unsafeCounter = 0;

    private static void demoNoLockVsSpinLock() throws InterruptedException {
        System.out.println("=".repeat(70));
        System.out.println("DEMO 5: Without Lock (Race Condition) vs With Spin Lock");
        System.out.println("  Demonstrates why we need the spin lock at all");
        System.out.println("=".repeat(70));

        int numThreads = 4;
        int incrementsPerThread = 250_000;
        int expected = numThreads * incrementsPerThread;

        // Without lock
        unsafeCounter = 0;
        Thread[] unsafeThreads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            unsafeThreads[i] = new Thread(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    unsafeCounter++;
                }
            });
        }
        for (Thread t : unsafeThreads) t.start();
        for (Thread t : unsafeThreads) t.join();

        System.out.printf("  Without lock: expected=%,d  actual=%,d  %s%n",
                expected, unsafeCounter,
                unsafeCounter == expected ? "CORRECT (got lucky!)" : "RACE CONDITION (data lost!)");

        // With spin lock
        TASSpinLock spinLock = new TASSpinLock();
        tasCounter = 0;
        Thread[] safeThreads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            safeThreads[i] = new Thread(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    spinLock.lock();
                    try {
                        tasCounter++;
                    } finally {
                        spinLock.unlock();
                    }
                }
            });
        }
        for (Thread t : safeThreads) t.start();
        for (Thread t : safeThreads) t.join();

        System.out.printf("  With spin lock: expected=%,d  actual=%,d  %s%n",
                expected, tasCounter,
                tasCounter == expected ? "CORRECT (spin lock protects the counter)" : "ERROR");
        System.out.println();
    }

    // ==================== MAIN ====================

    public static void main(String[] args) throws InterruptedException {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║              SPIN LOCK — Comprehensive Demo Suite                    ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════╝");
        System.out.println();

        demoTASSpinLock();
        demoTTASSpinLock();
        demoSpinLockDetailed();
        demoPerformanceComparison();
        demoNoLockVsSpinLock();

        System.out.println("=".repeat(70));
        System.out.println("All demos completed successfully!");
        System.out.println("=".repeat(70));
    }
}
