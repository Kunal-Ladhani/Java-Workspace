package com.scaler.concurrency.wait_notify_join_sleep;

/**
 * Proves the KEY difference between sleep() and wait():
 *
 *   DEMO 1: sleep() does NOT release the lock
 *           → Thread-B is BLOCKED the entire time Thread-A sleeps.
 *
 *   DEMO 2: wait() DOES release the lock
 *           → Thread-B can enter the synchronized block while Thread-A is waiting.
 *
 * Timestamps are printed to prove the behavior.
 * Run main() and compare the timing in the output.
 */
public class SleepVsWaitDemo {

    private static final Object lock = new Object();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========================================");
        System.out.println("  DEMO 1: sleep() Does NOT Release Lock");
        System.out.println("========================================");
        System.out.println("Expect: Thread-B is BLOCKED for ~2 seconds while Thread-A sleeps.\n");
        demoSleepDoesNotReleaseLock();

        Thread.sleep(500);

        System.out.println("\n========================================");
        System.out.println("  DEMO 2: wait() DOES Release Lock");
        System.out.println("========================================");
        System.out.println("Expect: Thread-B enters synchronized IMMEDIATELY while Thread-A waits.\n");
        demoWaitReleasesLock();
    }

    // ─── DEMO 1: sleep() holds the lock ──────────────────────────────────

    private static void demoSleepDoesNotReleaseLock() throws InterruptedException {
        final long startTime = System.currentTimeMillis();

        Thread threadA = new Thread(() -> {
            synchronized (lock) {
                log(startTime, "Thread-A", "Acquired lock. Calling Thread.sleep(2000)...");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                log(startTime, "Thread-A", "Woke up from sleep. Releasing lock now.");
            }
        }, "Thread-A");

        Thread threadB = new Thread(() -> {
            log(startTime, "Thread-B", "Trying to acquire lock...");
            synchronized (lock) {
                log(startTime, "Thread-B", "Acquired lock!");
                log(startTime, "Thread-B", "^ Notice: ~2000ms delay. Thread-A held the lock during sleep().");
            }
        }, "Thread-B");

        threadA.start();
        Thread.sleep(50); // small delay so Thread-A acquires lock first
        threadB.start();

        threadA.join();
        threadB.join();

        System.out.println();
        System.out.println("CONCLUSION: Thread-B was BLOCKED for the entire duration of Thread-A's sleep.");
        System.out.println("           sleep() does NOT release the monitor lock.");
    }

    // ─── DEMO 2: wait() releases the lock ────────────────────────────────

    private static void demoWaitReleasesLock() throws InterruptedException {
        final long startTime = System.currentTimeMillis();

        Thread threadA = new Thread(() -> {
            synchronized (lock) {
                log(startTime, "Thread-A", "Acquired lock. Calling lock.wait(2000)...");
                try {
                    lock.wait(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                log(startTime, "Thread-A", "Returned from wait(). Re-acquired lock.");
            }
        }, "Thread-A");

        Thread threadB = new Thread(() -> {
            log(startTime, "Thread-B", "Trying to acquire lock...");
            synchronized (lock) {
                log(startTime, "Thread-B", "Acquired lock!");
                log(startTime, "Thread-B", "^ Notice: almost ZERO delay. wait() released the lock immediately.");
                lock.notify();
                log(startTime, "Thread-B", "Called notify(). Releasing lock now.");
            }
        }, "Thread-B");

        threadA.start();
        Thread.sleep(50); // small delay so Thread-A acquires lock first
        threadB.start();

        threadA.join();
        threadB.join();

        System.out.println();
        System.out.println("CONCLUSION: Thread-B acquired the lock almost immediately (~50ms).");
        System.out.println("           wait() RELEASES the monitor lock, allowing other threads in.");
    }

    // ─── Utility: timestamped logger ─────────────────────────────────────

    private static void log(long startTime, String thread, String message) {
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.printf("[%4dms] [%-8s] %s%n", elapsed, thread, message);
    }
}
