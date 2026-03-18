package com.scaler.concurrency.pessimistic_lock;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Demonstrates Pessimistic Locking patterns:
 *   1. Unprotected inventory (race conditions — data corruption)
 *   2. Pessimistic locking with synchronized (correct behavior)
 *   3. Pessimistic locking with ReentrantLock (correct behavior)
 *   4. Side-by-side comparison: unprotected vs protected
 *
 * Run main() and observe the console output.
 */
public class PessimisticLockDemo {

    static final int INITIAL_STOCK = 100;
    static final int NUM_THREADS = 20;
    static final int BOOKINGS_PER_THREAD = 10;

    // ──────────────────────────────────────────────────────────────
    //  Unprotected Inventory — NO locking (demonstrates race condition)
    // ──────────────────────────────────────────────────────────────
    static class UnsafeInventory {
        private int stock;
        private int successfulBookings = 0;

        UnsafeInventory(int initialStock) {
            this.stock = initialStock;
        }

        public boolean book() {
            if (stock > 0) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                stock--;
                successfulBookings++;
                return true;
            }
            return false;
        }

        public int getStock() { return stock; }
        public int getSuccessfulBookings() { return successfulBookings; }
    }

    // ──────────────────────────────────────────────────────────────
    //  Pessimistic Inventory using synchronized
    // ──────────────────────────────────────────────────────────────
    static class SynchronizedInventory {
        private int stock;
        private int successfulBookings = 0;

        SynchronizedInventory(int initialStock) {
            this.stock = initialStock;
        }

        public synchronized boolean book() {
            if (stock > 0) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                stock--;
                successfulBookings++;
                return true;
            }
            return false;
        }

        public synchronized int getStock() { return stock; }
        public int getSuccessfulBookings() { return successfulBookings; }
    }

    // ──────────────────────────────────────────────────────────────
    //  Pessimistic Inventory using ReentrantLock
    // ──────────────────────────────────────────────────────────────
    static class ReentrantLockInventory {
        private int stock;
        private int successfulBookings = 0;
        private final ReentrantLock lock = new ReentrantLock();

        ReentrantLockInventory(int initialStock) {
            this.stock = initialStock;
        }

        public boolean book() {
            lock.lock();
            try {
                if (stock > 0) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    stock--;
                    successfulBookings++;
                    return true;
                }
                return false;
            } finally {
                lock.unlock();
            }
        }

        public int getStock() {
            lock.lock();
            try {
                return stock;
            } finally {
                lock.unlock();
            }
        }

        public int getSuccessfulBookings() { return successfulBookings; }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║         PESSIMISTIC LOCK DEMO — Inventory System        ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println("\nScenario: " + INITIAL_STOCK + " items in stock, "
                + NUM_THREADS + " threads each trying to book " + BOOKINGS_PER_THREAD + " items.");
        System.out.println("Maximum possible bookings = " + INITIAL_STOCK
                + " (stock should never go negative).\n");

        System.out.println("========================================");
        System.out.println("  DEMO 1: Unprotected (Race Condition)");
        System.out.println("========================================");
        demoUnsafe();

        Thread.sleep(500);

        System.out.println("\n========================================");
        System.out.println("  DEMO 2: Pessimistic — synchronized");
        System.out.println("========================================");
        demoSynchronized();

        Thread.sleep(500);

        System.out.println("\n========================================");
        System.out.println("  DEMO 3: Pessimistic — ReentrantLock");
        System.out.println("========================================");
        demoReentrantLock();

        Thread.sleep(500);

        System.out.println("\n========================================");
        System.out.println("  DEMO 4: Summary Comparison");
        System.out.println("========================================");
        demoSummary();
    }

    // ──────────────────────────────────────────────────────────────
    //  DEMO 1: No locking — expect race conditions
    // ──────────────────────────────────────────────────────────────
    private static void demoUnsafe() throws InterruptedException {
        UnsafeInventory inventory = new UnsafeInventory(INITIAL_STOCK);

        Thread[] threads = new Thread[NUM_THREADS];

        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < BOOKINGS_PER_THREAD; j++) {
                    inventory.book();
                }
            }, "Buyer-" + (i + 1));
        }

        long start = System.currentTimeMillis();
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        long elapsed = System.currentTimeMillis() - start;

        printResults("UNSAFE (no lock)", inventory.getStock(),
                inventory.getSuccessfulBookings(), elapsed);

        if (inventory.getStock() < 0) {
            System.out.println("  ⚠ STOCK WENT NEGATIVE! This is a race condition — ");
            System.out.println("    multiple threads read stock > 0 simultaneously,");
            System.out.println("    then ALL decremented it. Classic TOCTOU bug.");
        }
        if (inventory.getSuccessfulBookings() > INITIAL_STOCK) {
            System.out.println("  ⚠ MORE BOOKINGS THAN STOCK! " + inventory.getSuccessfulBookings()
                    + " bookings for " + INITIAL_STOCK + " items. Over-booking occurred.");
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  DEMO 2: synchronized — pessimistic, correct
    // ──────────────────────────────────────────────────────────────
    private static void demoSynchronized() throws InterruptedException {
        SynchronizedInventory inventory = new SynchronizedInventory(INITIAL_STOCK);

        Thread[] threads = new Thread[NUM_THREADS];
        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < BOOKINGS_PER_THREAD; j++) {
                    inventory.book();
                }
            }, "Buyer-" + (i + 1));
        }

        long start = System.currentTimeMillis();
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        long elapsed = System.currentTimeMillis() - start;

        printResults("SYNCHRONIZED", inventory.getStock(),
                inventory.getSuccessfulBookings(), elapsed);
        verifyCorrectness("synchronized", inventory.getStock(), inventory.getSuccessfulBookings());
    }

    // ──────────────────────────────────────────────────────────────
    //  DEMO 3: ReentrantLock — pessimistic, correct
    // ──────────────────────────────────────────────────────────────
    private static void demoReentrantLock() throws InterruptedException {
        ReentrantLockInventory inventory = new ReentrantLockInventory(INITIAL_STOCK);

        Thread[] threads = new Thread[NUM_THREADS];
        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < BOOKINGS_PER_THREAD; j++) {
                    inventory.book();
                }
            }, "Buyer-" + (i + 1));
        }

        long start = System.currentTimeMillis();
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        long elapsed = System.currentTimeMillis() - start;

        printResults("REENTRANT LOCK", inventory.getStock(),
                inventory.getSuccessfulBookings(), elapsed);
        verifyCorrectness("ReentrantLock", inventory.getStock(), inventory.getSuccessfulBookings());
    }

    // ──────────────────────────────────────────────────────────────
    //  DEMO 4: Summary
    // ──────────────────────────────────────────────────────────────
    private static void demoSummary() throws InterruptedException {
        UnsafeInventory unsafe = new UnsafeInventory(INITIAL_STOCK);
        SynchronizedInventory synced = new SynchronizedInventory(INITIAL_STOCK);
        ReentrantLockInventory locked = new ReentrantLockInventory(INITIAL_STOCK);

        runAll(unsafe, synced, locked);

        System.out.println("\n┌────────────────────────────────────────────────────────────────┐");
        System.out.println("│                    FINAL COMPARISON                            │");
        System.out.println("├──────────────────┬───────────┬────────────┬────────────────────┤");
        System.out.println("│ Approach         │ Final Stock│ Bookings  │ Correct?           │");
        System.out.println("├──────────────────┼───────────┼────────────┼────────────────────┤");
        System.out.printf("│ No Lock (unsafe) │ %9d │ %10d │ %-18s │%n",
                unsafe.getStock(), unsafe.getSuccessfulBookings(),
                (unsafe.getStock() >= 0 && unsafe.getSuccessfulBookings() <= INITIAL_STOCK) ? "Maybe (lucky)" : "NO — CORRUPT");
        System.out.printf("│ synchronized     │ %9d │ %10d │ %-18s │%n",
                synced.getStock(), synced.getSuccessfulBookings(),
                "YES");
        System.out.printf("│ ReentrantLock    │ %9d │ %10d │ %-18s │%n",
                locked.getStock(), locked.getSuccessfulBookings(),
                "YES");
        System.out.println("├──────────────────┴───────────┴────────────┴────────────────────┤");
        System.out.println("│ Expected: stock = 0, bookings = " + INITIAL_STOCK
                + " (all items booked, none over-booked) │");
        System.out.println("└────────────────────────────────────────────────────────────────┘");

        System.out.println("\nKey Takeaways:");
        System.out.println("  1. Without locking: stock can go negative (over-booking) — DATA CORRUPTION.");
        System.out.println("  2. With pessimistic locking (synchronized/ReentrantLock): stock is always >= 0.");
        System.out.println("  3. Pessimistic locking serializes access — slower but CORRECT.");
        System.out.println("  4. The check-then-act pattern (if stock > 0 then decrement) is NOT atomic");
        System.out.println("     without a lock. This is the classic TOCTOU (Time-of-Check-Time-of-Use) bug.");
    }

    private static void runAll(UnsafeInventory unsafe, SynchronizedInventory synced,
                               ReentrantLockInventory locked) throws InterruptedException {
        Thread[] threads = new Thread[NUM_THREADS * 3];
        for (int i = 0; i < NUM_THREADS; i++) {
            final int idx = i;
            threads[idx] = new Thread(() -> {
                for (int j = 0; j < BOOKINGS_PER_THREAD; j++) unsafe.book();
            });
            threads[NUM_THREADS + idx] = new Thread(() -> {
                for (int j = 0; j < BOOKINGS_PER_THREAD; j++) synced.book();
            });
            threads[2 * NUM_THREADS + idx] = new Thread(() -> {
                for (int j = 0; j < BOOKINGS_PER_THREAD; j++) locked.book();
            });
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
    }

    private static void printResults(String label, int stock, int bookings, long elapsed) {
        System.out.println("\n  Results for " + label + ":");
        System.out.println("    Final stock:        " + stock);
        System.out.println("    Successful bookings: " + bookings);
        System.out.println("    Time taken:         " + elapsed + " ms");
        System.out.println("    Stock + Bookings:   " + (stock + bookings)
                + " (should equal " + INITIAL_STOCK + ")");
    }

    private static void verifyCorrectness(String label, int stock, int bookings) {
        boolean correct = stock >= 0
                && bookings <= INITIAL_STOCK
                && (stock + bookings) == INITIAL_STOCK;
        if (correct) {
            System.out.println("  CORRECT: stock >= 0, no over-booking, stock + bookings = " + INITIAL_STOCK);
        } else {
            System.out.println("  ERROR: Invariant violated with " + label + "!");
        }
    }
}
