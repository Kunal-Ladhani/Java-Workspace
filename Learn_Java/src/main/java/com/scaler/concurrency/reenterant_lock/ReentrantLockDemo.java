package com.scaler.concurrency.reenterant_lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Demonstrates the core features of ReentrantLock:
 *   1. Reentrancy (same thread acquiring lock multiple times)
 *   2. Fair vs Non-Fair lock behavior
 *   3. tryLock() — non-blocking acquisition
 *   4. tryLock(timeout) — timed acquisition
 *   5. lockInterruptibly() — interruptible acquisition
 *   6. Utility methods (getHoldCount, isHeldByCurrentThread, etc.)
 *
 * Run main() and read the console output alongside the code to learn.
 */
public class ReentrantLockDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========================================");
        System.out.println("  DEMO 1: Reentrancy");
        System.out.println("========================================");
        demoReentrancy();

        Thread.sleep(500);

        System.out.println("\n========================================");
        System.out.println("  DEMO 2: Fair vs Non-Fair");
        System.out.println("========================================");
        demoFairness();

        Thread.sleep(3000);

        System.out.println("\n========================================");
        System.out.println("  DEMO 3: tryLock() — Non-blocking");
        System.out.println("========================================");
        demoTryLock();

        Thread.sleep(2000);

        System.out.println("\n========================================");
        System.out.println("  DEMO 4: tryLock(timeout)");
        System.out.println("========================================");
        demoTryLockWithTimeout();

        Thread.sleep(4000);

        System.out.println("\n========================================");
        System.out.println("  DEMO 5: lockInterruptibly()");
        System.out.println("========================================");
        demoLockInterruptibly();

        Thread.sleep(3000);

        System.out.println("\n========================================");
        System.out.println("  DEMO 6: Utility Methods");
        System.out.println("========================================");
        demoUtilityMethods();
    }

    // ──────────────────────────────────────────────
    //  DEMO 1: Reentrancy
    //  The same thread can lock() multiple times.
    //  Each lock() increments the hold count.
    //  Each unlock() decrements it.
    //  Lock is truly released only when hold count = 0.
    // ──────────────────────────────────────────────
    private static void demoReentrancy() {
        ReentrantLock lock = new ReentrantLock();

        System.out.println("Before any lock   → holdCount = " + lock.getHoldCount());

        lock.lock();
        System.out.println("After 1st lock()  → holdCount = " + lock.getHoldCount());

        lock.lock();
        System.out.println("After 2nd lock()  → holdCount = " + lock.getHoldCount());

        lock.lock();
        System.out.println("After 3rd lock()  → holdCount = " + lock.getHoldCount());

        lock.unlock();
        System.out.println("After 1st unlock()→ holdCount = " + lock.getHoldCount());

        lock.unlock();
        System.out.println("After 2nd unlock()→ holdCount = " + lock.getHoldCount());

        lock.unlock();
        System.out.println("After 3rd unlock()→ holdCount = " + lock.getHoldCount());
        System.out.println("Lock released?    → isLocked = " + lock.isLocked());

        // Real-world reentrancy: method calling another method that uses the same lock
        System.out.println("\n--- Reentrancy in practice (method calling method) ---");
        ReentrantLock sharedLock = new ReentrantLock();
        outerMethod(sharedLock);
    }

    private static void outerMethod(ReentrantLock lock) {
        lock.lock();
        try {
            System.out.println("outerMethod() acquired lock. holdCount = " + lock.getHoldCount());
            innerMethod(lock);  // calls another method that also locks
            System.out.println("Back in outerMethod(). holdCount = " + lock.getHoldCount());
        } finally {
            lock.unlock();
        }
    }

    private static void innerMethod(ReentrantLock lock) {
        lock.lock(); // same thread, same lock — no deadlock thanks to reentrancy!
        try {
            System.out.println("  innerMethod() acquired same lock. holdCount = " + lock.getHoldCount());
        } finally {
            lock.unlock();
        }
    }

    // ──────────────────────────────────────────────
    //  DEMO 2: Fair vs Non-Fair
    //  Non-fair (default): new threads can "barge" ahead of waiting threads.
    //  Fair: strictly FIFO — longest-waiting thread gets the lock next.
    // ──────────────────────────────────────────────
    private static void demoFairness() throws InterruptedException {
        System.out.println("\n--- Non-Fair Lock (threads may execute in any order) ---");
        runFairnessTest(new ReentrantLock(false));

        Thread.sleep(2000);

        System.out.println("\n--- Fair Lock (threads execute in arrival order) ---");
        runFairnessTest(new ReentrantLock(true));
    }

    private static void runFairnessTest(ReentrantLock lock) throws InterruptedException {
        System.out.println("Fair = " + lock.isFair());

        // Hold the lock so all threads queue up
        lock.lock();

        for (int i = 1; i <= 5; i++) {
            final int threadNum = i;
            Thread t = new Thread(() -> {
                lock.lock();
                try {
                    System.out.println("  Thread-" + threadNum + " acquired lock");
                } finally {
                    lock.unlock();
                }
            }, "Thread-" + i);
            t.start();
            Thread.sleep(50); // stagger starts so arrival order is 1, 2, 3, 4, 5
        }

        Thread.sleep(300); // let all threads enqueue
        lock.unlock();     // release — watch the order they acquire!
        Thread.sleep(1000);
    }

    // ──────────────────────────────────────────────
    //  DEMO 3: tryLock() — Non-blocking
    //  Returns immediately: true if acquired, false if not.
    //  The thread NEVER blocks.
    // ──────────────────────────────────────────────
    private static void demoTryLock() throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();

        // Thread-A holds the lock for 1 second
        Thread threadA = new Thread(() -> {
            lock.lock();
            try {
                System.out.println("  [A] Holding lock for 1 second...");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
                System.out.println("  [A] Released lock.");
            }
        }, "Thread-A");

        // Thread-B tries to acquire without blocking
        Thread threadB = new Thread(() -> {
            boolean acquired = lock.tryLock();
            if (acquired) {
                try {
                    System.out.println("  [B] Got the lock! (unexpected if A is still holding)");
                } finally {
                    lock.unlock();
                }
            } else {
                System.out.println("  [B] tryLock() returned false — lock is busy. Moving on!");
            }
        }, "Thread-B");

        threadA.start();
        Thread.sleep(100); // ensure A has the lock
        threadB.start();
    }

    // ──────────────────────────────────────────────
    //  DEMO 4: tryLock(timeout) — Timed lock acquisition
    //  Waits up to the given time. Returns true/false.
    //  Also responds to thread interruption.
    // ──────────────────────────────────────────────
    private static void demoTryLockWithTimeout() throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();

        Thread holder = new Thread(() -> {
            lock.lock();
            try {
                System.out.println("  [Holder] Holding lock for 3 seconds...");
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
                System.out.println("  [Holder] Released lock.");
            }
        }, "Holder");

        // This thread will TIMEOUT (waits only 1 sec, but holder keeps for 3 sec)
        Thread impatient = new Thread(() -> {
            try {
                System.out.println("  [Impatient] Trying to acquire lock (max 1 second)...");
                boolean acquired = lock.tryLock(1, TimeUnit.SECONDS);
                if (acquired) {
                    try {
                        System.out.println("  [Impatient] Got it!");
                    } finally {
                        lock.unlock();
                    }
                } else {
                    System.out.println("  [Impatient] Timed out after 1 second. Giving up.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Impatient");

        // This thread will SUCCEED (waits up to 5 sec, holder releases in 3 sec)
        Thread patient = new Thread(() -> {
            try {
                System.out.println("  [Patient] Trying to acquire lock (max 5 seconds)...");
                boolean acquired = lock.tryLock(5, TimeUnit.SECONDS);
                if (acquired) {
                    try {
                        System.out.println("  [Patient] Got it after waiting!");
                    } finally {
                        lock.unlock();
                    }
                } else {
                    System.out.println("  [Patient] Timed out.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Patient");

        holder.start();
        Thread.sleep(100);
        impatient.start();
        patient.start();
    }

    // ──────────────────────────────────────────────
    //  DEMO 5: lockInterruptibly()
    //  Like lock(), but if the thread is interrupted
    //  while waiting, it throws InterruptedException
    //  instead of waiting forever.
    //  Useful for cancellation / timeouts.
    // ──────────────────────────────────────────────
    private static void demoLockInterruptibly() throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();

        lock.lock(); // main thread holds the lock

        Thread waiter = new Thread(() -> {
            try {
                System.out.println("  [Waiter] Trying lockInterruptibly()...");
                lock.lockInterruptibly(); // will block here
                try {
                    System.out.println("  [Waiter] Acquired lock.");
                } finally {
                    lock.unlock();
                }
            } catch (InterruptedException e) {
                System.out.println("  [Waiter] Interrupted while waiting! Gracefully exiting.");
            }
        }, "Waiter");

        waiter.start();
        Thread.sleep(1000);

        System.out.println("  [Main] Interrupting the waiter thread...");
        waiter.interrupt(); // this will cause lockInterruptibly() to throw

        waiter.join();
        lock.unlock();
        System.out.println("  [Main] Done. The waiter was freed by interruption, not by getting the lock.");
    }

    // ──────────────────────────────────────────────
    //  DEMO 6: Utility Methods
    //  ReentrantLock exposes useful query methods
    //  that synchronized does not.
    // ──────────────────────────────────────────────
    private static void demoUtilityMethods() throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();

        System.out.println("isLocked()              = " + lock.isLocked());
        System.out.println("isHeldByCurrentThread() = " + lock.isHeldByCurrentThread());
        System.out.println("getHoldCount()          = " + lock.getHoldCount());

        lock.lock();
        lock.lock(); // reentrant

        System.out.println("\nAfter main thread locks twice:");
        System.out.println("isLocked()              = " + lock.isLocked());
        System.out.println("isHeldByCurrentThread() = " + lock.isHeldByCurrentThread());
        System.out.println("getHoldCount()          = " + lock.getHoldCount());
        System.out.println("getQueueLength()        = " + lock.getQueueLength());

        Thread blocked = new Thread(() -> {
            lock.lock();
            try { /* no-op */ } finally { lock.unlock(); }
        });
        blocked.start();
        Thread.sleep(200);

        System.out.println("\nAfter another thread tries to acquire:");
        System.out.println("getQueueLength()        = " + lock.getQueueLength());
        System.out.println("hasQueuedThreads()      = " + lock.hasQueuedThreads());
        System.out.println("hasQueuedThread(blocked) = " + lock.hasQueuedThread(blocked));

        lock.unlock();
        lock.unlock();
        blocked.join();

        System.out.println("\nAfter full unlock:");
        System.out.println("isLocked()              = " + lock.isLocked());
    }
}
