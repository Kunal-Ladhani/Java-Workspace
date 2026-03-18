package com.scaler.concurrency.mutex;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

public class MutexDemo {

    private static int sharedCounter = 0;

    // ==================== DEMO 1: Mutex via synchronized ====================

    private static final Object syncLock = new Object();

    private static void demoSynchronized() throws InterruptedException {
        System.out.println("=".repeat(70));
        System.out.println("DEMO 1: Mutex via synchronized keyword");
        System.out.println("  Two threads incrementing a shared counter 100,000 times each");
        System.out.println("=".repeat(70));

        sharedCounter = 0;

        Runnable task = () -> {
            for (int i = 0; i < 100_000; i++) {
                synchronized (syncLock) {
                    sharedCounter++;
                }
            }
        };

        Thread t1 = new Thread(task, "Thread-1");
        Thread t2 = new Thread(task, "Thread-2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("  Expected: 200000");
        System.out.println("  Actual:   " + sharedCounter);
        System.out.println("  Result:   " + (sharedCounter == 200_000 ? "CORRECT" : "RACE CONDITION!"));

        System.out.println("\n  synchronized properties:");
        System.out.println("  - Ownership: only the entering thread can exit the block");
        System.out.println("  - Reentrant: same thread can enter nested synchronized blocks");
        System.out.println("  - Automatic release on exception or block exit");
        System.out.println();
    }

    // ==================== DEMO 2: Mutex via ReentrantLock ====================

    private static final ReentrantLock reentrantLock = new ReentrantLock();

    private static void demoReentrantLock() throws InterruptedException {
        System.out.println("=".repeat(70));
        System.out.println("DEMO 2: Mutex via ReentrantLock");
        System.out.println("  Demonstrating lock ownership, reentrancy, and hold count");
        System.out.println("=".repeat(70));

        sharedCounter = 0;

        Thread worker = new Thread(() -> {
            reentrantLock.lock();
            try {
                System.out.println("  [Worker] Acquired lock. Hold count: " + reentrantLock.getHoldCount());

                reentrantLock.lock();
                System.out.println("  [Worker] Re-acquired lock (reentrant). Hold count: "
                        + reentrantLock.getHoldCount());

                reentrantLock.lock();
                System.out.println("  [Worker] Re-acquired again. Hold count: "
                        + reentrantLock.getHoldCount());

                reentrantLock.unlock();
                System.out.println("  [Worker] One unlock. Hold count: " + reentrantLock.getHoldCount());

                reentrantLock.unlock();
                System.out.println("  [Worker] Two unlocks. Hold count: " + reentrantLock.getHoldCount());
            } finally {
                reentrantLock.unlock();
                System.out.println("  [Worker] Final unlock. Hold count: " + reentrantLock.getHoldCount());
            }
        }, "Worker");

        worker.start();
        worker.join();

        System.out.println("\n  ReentrantLock properties:");
        System.out.println("  - Tracks hold count for reentrancy");
        System.out.println("  - Must unlock the same number of times as lock()");
        System.out.println("  - Only owning thread can unlock");
        System.out.println();
    }

    // ==================== DEMO 3: Mutex via Semaphore(1) ====================

    private static void demoSemaphoreAsMutex() throws InterruptedException {
        System.out.println("=".repeat(70));
        System.out.println("DEMO 3: Semaphore(1) as Mutex (Not a True Mutex!)");
        System.out.println("  Showing it provides mutual exclusion BUT lacks ownership");
        System.out.println("=".repeat(70));

        Semaphore semMutex = new Semaphore(1);
        sharedCounter = 0;

        Runnable task = () -> {
            for (int i = 0; i < 100_000; i++) {
                try {
                    semMutex.acquire();
                    sharedCounter++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } finally {
                    semMutex.release();
                }
            }
        };

        Thread t1 = new Thread(task, "Thread-1");
        Thread t2 = new Thread(task, "Thread-2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("  Expected: 200000");
        System.out.println("  Actual:   " + sharedCounter);
        System.out.println("  Result:   " + (sharedCounter == 200_000 ? "CORRECT — mutual exclusion works" : "RACE CONDITION!"));

        System.out.println("\n  BUT — Semaphore(1) is NOT a true mutex:");
        System.out.println("  - Any thread can call release() (no ownership check)");
        System.out.println("  - Not reentrant (same thread acquiring twice = deadlock)");
        System.out.println("  - No hold count tracking");
        System.out.println();
    }

    // ==================== DEMO 4: Ownership Difference ====================

    private static void demoOwnershipDifference() throws InterruptedException {
        System.out.println("=".repeat(70));
        System.out.println("DEMO 4: Ownership Difference — ReentrantLock vs Semaphore(1)");
        System.out.println("  Thread-A acquires, Thread-B tries to release");
        System.out.println("=".repeat(70));

        // --- ReentrantLock: wrong-thread unlock throws exception ---
        System.out.println("\n  --- ReentrantLock (has ownership) ---");
        ReentrantLock lock = new ReentrantLock();

        Thread lockHolder = new Thread(() -> {
            lock.lock();
            System.out.println("  [Thread-A] Acquired ReentrantLock.");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
                System.out.println("  [Thread-A] Released ReentrantLock.");
            }
        }, "Thread-A");

        Thread lockIntruder = new Thread(() -> {
            try {
                Thread.sleep(200);
                System.out.println("  [Thread-B] Attempting to unlock ReentrantLock (not the owner)...");
                lock.unlock();
                System.out.println("  [Thread-B] Unlocked successfully? (should NOT reach here)");
            } catch (IllegalMonitorStateException e) {
                System.out.println("  [Thread-B] EXCEPTION: " + e.getClass().getSimpleName()
                        + " — \"" + e.getMessage() + "\"");
                System.out.println("  [Thread-B] ReentrantLock correctly enforces ownership!");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Thread-B");

        lockHolder.start();
        lockIntruder.start();
        lockHolder.join();
        lockIntruder.join();

        // --- Semaphore(1): wrong-thread release succeeds silently ---
        System.out.println("\n  --- Semaphore(1) (NO ownership) ---");
        Semaphore sem = new Semaphore(1);

        Thread semHolder = new Thread(() -> {
            try {
                sem.acquire();
                System.out.println("  [Thread-A] Acquired Semaphore permit. Permits: "
                        + sem.availablePermits());
                Thread.sleep(2000);
                sem.release();
                System.out.println("  [Thread-A] Released Semaphore permit. Permits: "
                        + sem.availablePermits());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Thread-A");

        Thread semIntruder = new Thread(() -> {
            try {
                Thread.sleep(200);
                System.out.println("  [Thread-B] Releasing Semaphore permit (not the acquirer)...");
                sem.release();
                System.out.println("  [Thread-B] Release succeeded! Permits now: "
                        + sem.availablePermits());
                System.out.println("  [Thread-B] DANGER: Semaphore has NO ownership check!");
                System.out.println("  [Thread-B] Another thread could now acquire — breaking mutual exclusion.");
            } catch (Exception e) {
                System.out.println("  [Thread-B] Exception: " + e.getMessage());
            }
        }, "Thread-B");

        semHolder.start();
        semIntruder.start();
        semHolder.join();
        semIntruder.join();

        System.out.println();
    }

    // ==================== DEMO 5: Reentrancy Comparison ====================

    private static void demoReentrancyComparison() throws InterruptedException {
        System.out.println("=".repeat(70));
        System.out.println("DEMO 5: Reentrancy — ReentrantLock vs Semaphore(1)");
        System.out.println("=".repeat(70));

        // ReentrantLock: reentrant
        System.out.println("\n  --- ReentrantLock (reentrant) ---");
        ReentrantLock lock = new ReentrantLock();

        Thread reentrantThread = new Thread(() -> {
            lock.lock();
            System.out.println("  [Lock] First lock. Hold count: " + lock.getHoldCount());
            lock.lock();
            System.out.println("  [Lock] Second lock (same thread). Hold count: " + lock.getHoldCount());
            System.out.println("  [Lock] No deadlock! ReentrantLock recognizes the owner.");
            lock.unlock();
            lock.unlock();
            System.out.println("  [Lock] Both unlocked. Hold count: " + lock.getHoldCount());
        });

        reentrantThread.start();
        reentrantThread.join();

        // Semaphore(1): NOT reentrant — would deadlock
        System.out.println("\n  --- Semaphore(1) (NOT reentrant) ---");
        Semaphore sem = new Semaphore(1);

        Thread semThread = new Thread(() -> {
            try {
                sem.acquire();
                System.out.println("  [Sem]  First acquire. Permits: " + sem.availablePermits());
                System.out.println("  [Sem]  Attempting second acquire (same thread)...");
                System.out.println("  [Sem]  *** SKIPPING to avoid deadlock in demo ***");
                System.out.println("  [Sem]  In real code: sem.acquire() here would DEADLOCK!");
                System.out.println("  [Sem]  Semaphore has no concept of 'owner' — it just sees permits=0.");
                sem.release();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        semThread.start();
        semThread.join();

        System.out.println();
    }

    // ==================== DEMO 6: synchronized Reentrancy ====================

    private static synchronized void outerMethod() {
        System.out.println("  [" + Thread.currentThread().getName() + "] In outerMethod — holding lock");
        innerMethod();
    }

    private static synchronized void innerMethod() {
        System.out.println("  [" + Thread.currentThread().getName() + "] In innerMethod — re-entered lock!");
        System.out.println("  [" + Thread.currentThread().getName() + "] synchronized is reentrant: no deadlock.");
    }

    private static void demoSynchronizedReentrancy() throws InterruptedException {
        System.out.println("=".repeat(70));
        System.out.println("DEMO 6: synchronized Reentrancy");
        System.out.println("  Showing that synchronized methods calling other synchronized methods work");
        System.out.println("=".repeat(70));

        Thread t = new Thread(MutexDemo::outerMethod, "Worker");
        t.start();
        t.join();

        System.out.println("\n  synchronized recognizes that the same thread holds the monitor.");
        System.out.println("  This is a true mutex property — ownership + reentrancy.\n");
    }

    // ==================== MAIN ====================

    public static void main(String[] args) throws InterruptedException {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                 MUTEX — Comprehensive Demo Suite                     ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════╝");
        System.out.println();

        demoSynchronized();
        demoReentrantLock();
        demoSemaphoreAsMutex();
        demoOwnershipDifference();
        demoReentrancyComparison();
        demoSynchronizedReentrancy();

        System.out.println("=".repeat(70));
        System.out.println("All demos completed successfully!");
        System.out.println("=".repeat(70));
    }
}
