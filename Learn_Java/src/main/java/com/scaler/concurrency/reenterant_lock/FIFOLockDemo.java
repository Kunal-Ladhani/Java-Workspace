package com.scaler.concurrency.reenterant_lock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Demonstrates FIFO lock ordering:
 *   1. Custom TicketLock implementation (always FIFO)
 *   2. TicketLock vs Non-Fair ReentrantLock ordering comparison
 *   3. Fair ReentrantLock showing FIFO behavior
 *
 * Run main() and observe the console output.
 */
public class FIFOLockDemo {

    // ──────────────────────────────────────────────────────────────
    //  Custom TicketLock — a classic FIFO spinlock.
    //  Like a deli counter: take a number, wait for your turn.
    // ──────────────────────────────────────────────────────────────
    static class TicketLock {
        private final AtomicInteger ticketCounter = new AtomicInteger(0);
        private final AtomicInteger servingCounter = new AtomicInteger(0);

        public int lock() {
            int myTicket = ticketCounter.getAndIncrement();
            while (servingCounter.get() != myTicket) {
                Thread.onSpinWait();
            }
            return myTicket;
        }

        public void unlock() {
            servingCounter.incrementAndGet();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║          FIFO LOCK DEMO — Ordering Guarantees       ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");

        System.out.println("\n========================================");
        System.out.println("  DEMO 1: TicketLock — Always FIFO");
        System.out.println("========================================");
        demoTicketLock();

        Thread.sleep(1000);

        System.out.println("\n========================================");
        System.out.println("  DEMO 2: Non-Fair ReentrantLock");
        System.out.println("  (NOT FIFO — barging allowed)");
        System.out.println("========================================");
        demoNonFairReentrantLock();

        Thread.sleep(1000);

        System.out.println("\n========================================");
        System.out.println("  DEMO 3: Fair ReentrantLock");
        System.out.println("  (FIFO — same as TicketLock ordering)");
        System.out.println("========================================");
        demoFairReentrantLock();

        Thread.sleep(500);

        System.out.println("\n========================================");
        System.out.println("  DEMO 4: Side-by-Side Comparison");
        System.out.println("========================================");
        demoSideBySideComparison();
    }

    // ──────────────────────────────────────────────────────────────
    //  DEMO 1: TicketLock
    //  8 threads compete. Each takes a ticket number.
    //  Acquisition order ALWAYS matches ticket order.
    // ──────────────────────────────────────────────────────────────
    private static void demoTicketLock() throws InterruptedException {
        TicketLock ticketLock = new TicketLock();
        List<String> order = Collections.synchronizedList(new ArrayList<>());
        int numThreads = 8;

        ticketLock.lock();

        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            final int id = i + 1;
            threads[i] = new Thread(() -> {
                int ticket = ticketLock.lock();
                try {
                    String entry = "T" + id + "(ticket=" + ticket + ")";
                    order.add(entry);
                    System.out.println("  " + entry + " acquired lock");
                } finally {
                    ticketLock.unlock();
                }
            }, "Thread-" + id);
            threads[i].start();
            Thread.sleep(20);
        }

        Thread.sleep(200);
        System.out.println("\nReleasing lock — threads will acquire in ticket order:\n");
        ticketLock.unlock();

        for (Thread t : threads) {
            t.join();
        }

        System.out.println("\nTicketLock order: " + order);
        System.out.println("→ Threads acquired in exact ticket order (FIFO guaranteed).");
    }

    // ──────────────────────────────────────────────────────────────
    //  DEMO 2: Non-Fair ReentrantLock
    //  8 threads compete. Barging can reorder acquisitions.
    // ──────────────────────────────────────────────────────────────
    private static void demoNonFairReentrantLock() throws InterruptedException {
        ReentrantLock lock = new ReentrantLock(false);
        List<String> order = Collections.synchronizedList(new ArrayList<>());
        int numThreads = 8;

        lock.lock();

        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            final int id = i + 1;
            threads[i] = new Thread(() -> {
                lock.lock();
                try {
                    String entry = "T" + id;
                    order.add(entry);
                    System.out.println("  " + entry + " acquired lock");
                } finally {
                    lock.unlock();
                }
            }, "Thread-" + id);
            threads[i].start();
            Thread.sleep(20);
        }

        Thread.sleep(200);
        System.out.println("\nReleasing lock — order may vary due to barging:\n");
        lock.unlock();

        for (Thread t : threads) {
            t.join();
        }

        System.out.println("\nNon-Fair order: " + order);
        System.out.println("→ Order may NOT match arrival order (barging allowed).");
    }

    // ──────────────────────────────────────────────────────────────
    //  DEMO 3: Fair ReentrantLock
    //  8 threads compete. Strict FIFO — same as TicketLock.
    // ──────────────────────────────────────────────────────────────
    private static void demoFairReentrantLock() throws InterruptedException {
        ReentrantLock lock = new ReentrantLock(true);
        List<String> order = Collections.synchronizedList(new ArrayList<>());
        int numThreads = 8;

        lock.lock();

        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            final int id = i + 1;
            threads[i] = new Thread(() -> {
                lock.lock();
                try {
                    String entry = "T" + id;
                    order.add(entry);
                    System.out.println("  " + entry + " acquired lock");
                } finally {
                    lock.unlock();
                }
            }, "Thread-" + id);
            threads[i].start();
            Thread.sleep(20);
        }

        Thread.sleep(200);
        System.out.println("\nReleasing lock — strict FIFO order:\n");
        lock.unlock();

        for (Thread t : threads) {
            t.join();
        }

        System.out.println("\nFair lock order: " + order);
        System.out.println("→ Threads acquired in exact arrival order (FIFO, same as TicketLock).");
    }

    // ──────────────────────────────────────────────────────────────
    //  DEMO 4: Side-by-Side Summary
    //  Run all three approaches and print a comparison.
    // ──────────────────────────────────────────────────────────────
    private static void demoSideBySideComparison() throws InterruptedException {
        int numThreads = 6;

        List<String> ticketOrder = runWithTicketLock(numThreads);
        List<String> fairOrder = runWithReentrantLock(numThreads, true);
        List<String> nonFairOrder = runWithReentrantLock(numThreads, false);

        System.out.println("\n┌─────────────────────────────────────────────────────┐");
        System.out.println("│              SIDE-BY-SIDE COMPARISON                │");
        System.out.println("├─────────────────────────────────────────────────────┤");
        System.out.println("│ Arrival order:      T1 → T2 → T3 → T4 → T5 → T6   │");
        System.out.println("├─────────────────────────────────────────────────────┤");
        System.out.printf("│ TicketLock (FIFO):  %-33s│%n", ticketOrder);
        System.out.printf("│ Fair ReentrantLock: %-33s│%n", fairOrder);
        System.out.printf("│ Non-Fair Lock:      %-33s│%n", nonFairOrder);
        System.out.println("├─────────────────────────────────────────────────────┤");
        System.out.println("│ TicketLock & Fair lock: always T1→T2→T3→T4→T5→T6   │");
        System.out.println("│ Non-Fair lock: order may vary (barging)             │");
        System.out.println("└─────────────────────────────────────────────────────┘");
    }

    private static List<String> runWithTicketLock(int numThreads) throws InterruptedException {
        TicketLock lock = new TicketLock();
        List<String> order = Collections.synchronizedList(new ArrayList<>());

        lock.lock();

        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            final int id = i + 1;
            threads[i] = new Thread(() -> {
                lock.lock();
                try {
                    order.add("T" + id);
                } finally {
                    lock.unlock();
                }
            });
            threads[i].start();
            Thread.sleep(15);
        }

        Thread.sleep(150);
        lock.unlock();

        for (Thread t : threads) {
            t.join();
        }
        return order;
    }

    private static List<String> runWithReentrantLock(int numThreads, boolean fair) throws InterruptedException {
        ReentrantLock lock = new ReentrantLock(fair);
        List<String> order = Collections.synchronizedList(new ArrayList<>());

        lock.lock();

        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            final int id = i + 1;
            threads[i] = new Thread(() -> {
                lock.lock();
                try {
                    order.add("T" + id);
                } finally {
                    lock.unlock();
                }
            });
            threads[i].start();
            Thread.sleep(15);
        }

        Thread.sleep(150);
        lock.unlock();

        for (Thread t : threads) {
            t.join();
        }
        return order;
    }
}
