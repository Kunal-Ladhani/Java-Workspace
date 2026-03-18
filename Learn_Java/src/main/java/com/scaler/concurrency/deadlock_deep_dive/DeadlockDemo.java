package com.scaler.concurrency.deadlock_deep_dive;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates deadlock creation, detection, prevention, and livelock.
 */
public class DeadlockDemo {

    // ──────────────────────────────────────────────────────────────
    // Section 1: Deliberate deadlock with ThreadMXBean detection
    // ──────────────────────────────────────────────────────────────

    private static final Object lockA = new Object();
    private static final Object lockB = new Object();

    private static void demoDeadlockWithDetection() throws InterruptedException {
        System.out.println("==========================================================");
        System.out.println("  DEMO 1: Deliberate Deadlock + ThreadMXBean Detection");
        System.out.println("==========================================================");
        System.out.println("Two threads acquire two locks in opposite order.\n");

        Thread thread1 = new Thread(() -> {
            synchronized (lockA) {
                System.out.println("[Thread-1] Acquired Lock A, waiting for Lock B...");
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                synchronized (lockB) {
                    System.out.println("[Thread-1] Acquired Lock B (won't reach here).");
                }
            }
        }, "Thread-1");

        Thread thread2 = new Thread(() -> {
            synchronized (lockB) {
                System.out.println("[Thread-2] Acquired Lock B, waiting for Lock A...");
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                synchronized (lockA) {
                    System.out.println("[Thread-2] Acquired Lock A (won't reach here).");
                }
            }
        }, "Thread-2");

        thread1.start();
        thread2.start();

        Thread.sleep(1000);

        System.out.println("\n[Detector] Checking for deadlocks via ThreadMXBean...");
        ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();
        long[] deadlockedIds = mxBean.findDeadlockedThreads();

        if (deadlockedIds != null) {
            System.out.println("[Detector] DEADLOCK DETECTED! Involved threads:\n");
            ThreadInfo[] infos = mxBean.getThreadInfo(deadlockedIds, true, true);
            for (ThreadInfo info : infos) {
                System.out.println("  Thread: " + info.getThreadName());
                System.out.println("  State:  " + info.getThreadState());
                System.out.println("  Waiting for lock: " + info.getLockName());
                System.out.println("  Lock held by: " + info.getLockOwnerName());
                System.out.println("  Stack trace (top 3 frames):");
                StackTraceElement[] stack = info.getStackTrace();
                for (int i = 0; i < Math.min(3, stack.length); i++) {
                    System.out.println("    at " + stack[i]);
                }
                System.out.println();
            }
        } else {
            System.out.println("[Detector] No deadlock found (unexpected).");
        }

        System.out.println("[Main] Stopping deadlocked threads (daemon-like cleanup)...");
        thread1.interrupt();
        thread2.interrupt();
        thread1.join(500);
        thread2.join(500);
        if (thread1.isAlive() || thread2.isAlive()) {
            System.out.println("[Main] Threads still stuck (synchronized doesn't respond to interrupt).");
            System.out.println("[Main] They will be cleaned up when JVM exits.\n");
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Section 2: Deadlock prevention via lock ordering
    // ──────────────────────────────────────────────────────────────

    private static final Object resource1 = new Object();
    private static final Object resource2 = new Object();

    private static void acquireInOrder(Object a, Object b, String threadName) {
        int hashA = System.identityHashCode(a);
        int hashB = System.identityHashCode(b);

        Object first = hashA < hashB ? a : b;
        Object second = hashA < hashB ? b : a;

        synchronized (first) {
            System.out.println("[" + threadName + "] Acquired first lock (hash=" + System.identityHashCode(first) + ")");
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            synchronized (second) {
                System.out.println("[" + threadName + "] Acquired second lock (hash=" + System.identityHashCode(second) + ") — SUCCESS!");
            }
        }
    }

    private static void demoLockOrdering() throws InterruptedException {
        System.out.println("==========================================================");
        System.out.println("  DEMO 2: Deadlock Prevention via Lock Ordering");
        System.out.println("==========================================================");
        System.out.println("Both threads acquire locks in the same order (by hash).\n");

        Thread t1 = new Thread(() -> acquireInOrder(resource1, resource2, "Thread-A"), "Thread-A");
        Thread t2 = new Thread(() -> acquireInOrder(resource2, resource1, "Thread-B"), "Thread-B");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("\n[Main] Both threads completed — no deadlock!\n");
    }

    // ──────────────────────────────────────────────────────────────
    // Section 3: tryLock with timeout (break hold-and-wait)
    // ──────────────────────────────────────────────────────────────

    private static final ReentrantLock rLock1 = new ReentrantLock();
    private static final ReentrantLock rLock2 = new ReentrantLock();

    private static boolean tryAcquireBothLocks(ReentrantLock first, ReentrantLock second,
                                                String threadName) throws InterruptedException {
        boolean gotFirst = false;
        boolean gotSecond = false;
        try {
            gotFirst = first.tryLock(200, TimeUnit.MILLISECONDS);
            if (gotFirst) {
                System.out.println("[" + threadName + "] Acquired first lock, trying second...");
                Thread.sleep(50);
                gotSecond = second.tryLock(200, TimeUnit.MILLISECONDS);
                if (gotSecond) {
                    System.out.println("[" + threadName + "] Acquired both locks — doing work.");
                    return true;
                } else {
                    System.out.println("[" + threadName + "] Could not acquire second lock — backing off.");
                }
            } else {
                System.out.println("[" + threadName + "] Could not acquire first lock — backing off.");
            }
        } finally {
            if (gotSecond) second.unlock();
            if (gotFirst) first.unlock();
        }
        return false;
    }

    private static void demoTryLock() throws InterruptedException {
        System.out.println("==========================================================");
        System.out.println("  DEMO 3: tryLock with Timeout — Break Hold-and-Wait");
        System.out.println("==========================================================");
        System.out.println("Threads try to acquire locks in opposite order, but use");
        System.out.println("tryLock with timeout to avoid deadlock.\n");

        Thread t1 = new Thread(() -> {
            try {
                for (int attempt = 1; attempt <= 5; attempt++) {
                    System.out.println("[Thread-X] Attempt " + attempt);
                    if (tryAcquireBothLocks(rLock1, rLock2, "Thread-X")) {
                        return;
                    }
                    Thread.sleep((long) (Math.random() * 100));
                }
                System.out.println("[Thread-X] Gave up after 5 attempts.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Thread-X");

        Thread t2 = new Thread(() -> {
            try {
                for (int attempt = 1; attempt <= 5; attempt++) {
                    System.out.println("[Thread-Y] Attempt " + attempt);
                    if (tryAcquireBothLocks(rLock2, rLock1, "Thread-Y")) {
                        return;
                    }
                    Thread.sleep((long) (Math.random() * 100));
                }
                System.out.println("[Thread-Y] Gave up after 5 attempts.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Thread-Y");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("\n[Main] Both threads finished — no permanent deadlock!\n");
    }

    // ──────────────────────────────────────────────────────────────
    // Section 4: Livelock demonstration
    // ──────────────────────────────────────────────────────────────

    static class Polite {
        private final String name;
        private boolean wantsToAct;

        Polite(String name) {
            this.name = name;
            this.wantsToAct = true;
        }

        public String getName() { return name; }
        public boolean wantsToAct() { return wantsToAct; }
        public void setWantsToAct(boolean wants) { this.wantsToAct = wants; }
    }

    private static void demoLivelock() throws InterruptedException {
        System.out.println("==========================================================");
        System.out.println("  DEMO 4: Livelock Demonstration");
        System.out.println("==========================================================");
        System.out.println("Two 'polite' threads keep yielding to each other.");
        System.out.println("Both are active (not blocked) but neither makes progress.\n");

        final Polite alice = new Polite("Alice");
        final Polite bob = new Polite("Bob");
        final int maxYields = 10;

        Thread aliceThread = new Thread(() -> {
            int yields = 0;
            while (alice.wantsToAct() && yields < maxYields) {
                if (bob.wantsToAct()) {
                    System.out.println("[Alice] 'Oh, you go first, Bob!' (yielding)");
                    alice.setWantsToAct(false);
                    try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                    alice.setWantsToAct(true);
                    yields++;
                    continue;
                }
                System.out.println("[Alice] Finally acting! (Bob stepped aside)");
                alice.setWantsToAct(false);
                return;
            }
            System.out.println("[Alice] Gave up after " + yields + " yields (LIVELOCK detected by limit).");
        }, "Alice");

        Thread bobThread = new Thread(() -> {
            int yields = 0;
            while (bob.wantsToAct() && yields < maxYields) {
                if (alice.wantsToAct()) {
                    System.out.println("[Bob]   'Oh, you go first, Alice!' (yielding)");
                    bob.setWantsToAct(false);
                    try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                    bob.setWantsToAct(true);
                    yields++;
                    continue;
                }
                System.out.println("[Bob]   Finally acting! (Alice stepped aside)");
                bob.setWantsToAct(false);
                return;
            }
            System.out.println("[Bob]   Gave up after " + yields + " yields (LIVELOCK detected by limit).");
        }, "Bob");

        aliceThread.start();
        bobThread.start();
        aliceThread.join();
        bobThread.join();

        System.out.println("\n[Main] Livelock demo completed.");
        System.out.println("  → Both threads were ACTIVE (not blocked) but made no useful progress.");
        System.out.println("  → Fix: use random backoff so they don't keep yielding in lockstep.\n");
    }

    // ──────────────────────────────────────────────────────────────
    // Main
    // ──────────────────────────────────────────────────────────────

    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║           DEADLOCK DEEP DIVE — JAVA DEMO                ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");

        demoDeadlockWithDetection();
        demoLockOrdering();
        demoTryLock();
        demoLivelock();

        System.out.println("==========================================================");
        System.out.println("  All demos completed.");
        System.out.println("==========================================================");
    }
}
