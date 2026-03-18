package com.scaler.concurrency.wait_notify_join_sleep;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Demonstrates sleep(), join(), wait/notify, lost signals, and spurious wakeup protection.
 *
 *   DEMO 1: sleep() does NOT release locks
 *   DEMO 2: join() — waiting for worker threads
 *   DEMO 3: wait/notify — producer-consumer
 *   DEMO 4: Lost signal — notify() before wait()
 *   DEMO 5: Spurious wakeup protection — while vs if
 *
 * Run main() and read the console output alongside the code.
 */
public class WaitNotifyDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========================================");
        System.out.println("  DEMO 1: sleep() Does NOT Release Lock");
        System.out.println("========================================");
        demoSleepHoldsLock();

        Thread.sleep(500);

        System.out.println("\n========================================");
        System.out.println("  DEMO 2: join() — Wait for Workers");
        System.out.println("========================================");
        demoJoin();

        Thread.sleep(500);

        System.out.println("\n========================================");
        System.out.println("  DEMO 3: wait/notify — Producer-Consumer");
        System.out.println("========================================");
        demoProducerConsumer();

        Thread.sleep(500);

        System.out.println("\n========================================");
        System.out.println("  DEMO 4: Lost Signal Problem");
        System.out.println("========================================");
        demoLostSignal();

        Thread.sleep(500);

        System.out.println("\n========================================");
        System.out.println("  DEMO 5: Spurious Wakeup Protection");
        System.out.println("========================================");
        demoSpuriousWakeupProtection();
    }

    // ─── DEMO 1: sleep() holds the lock ──────────────────────────────────

    private static void demoSleepHoldsLock() throws InterruptedException {
        final Object lock = new Object();

        Thread sleeper = new Thread(() -> {
            synchronized (lock) {
                System.out.println("[Sleeper] Acquired lock, going to sleep for 2 seconds...");
                long start = System.currentTimeMillis();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                long elapsed = System.currentTimeMillis() - start;
                System.out.println("[Sleeper] Woke up after " + elapsed + "ms. Still held the lock the ENTIRE time.");
            }
        }, "Sleeper");

        Thread blocked = new Thread(() -> {
            long start = System.currentTimeMillis();
            System.out.println("[Blocked] Trying to acquire lock...");
            synchronized (lock) {
                long waited = System.currentTimeMillis() - start;
                System.out.println("[Blocked] Finally acquired lock after " + waited + "ms!");
                System.out.println("[Blocked] This proves sleep() does NOT release the lock.");
            }
        }, "Blocked");

        sleeper.start();
        Thread.sleep(100); // ensure sleeper acquires lock first
        blocked.start();

        sleeper.join();
        blocked.join();
    }

    // ─── DEMO 2: join() ──────────────────────────────────────────────────

    private static void demoJoin() throws InterruptedException {
        Thread[] workers = new Thread[3];

        for (int i = 0; i < 3; i++) {
            final int id = i + 1;
            workers[i] = new Thread(() -> {
                System.out.println("[Worker-" + id + "] Started. Working...");
                try {
                    Thread.sleep(500 + (id * 300));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("[Worker-" + id + "] Finished after " + (500 + id * 300) + "ms.");
            }, "Worker-" + id);
        }

        long start = System.currentTimeMillis();
        for (Thread w : workers) {
            w.start();
        }

        System.out.println("[Main] All workers started. Now joining all...");
        for (Thread w : workers) {
            w.join();
            System.out.println("[Main] " + w.getName() + " has been joined.");
        }

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("[Main] All workers done! Total time: " + elapsed + "ms");
        System.out.println("[Main] Note: total ≈ longest worker, NOT sum of all, because they ran in parallel.");
    }

    // ─── DEMO 3: Producer-Consumer with wait/notify ──────────────────────

    private static void demoProducerConsumer() throws InterruptedException {
        final Queue<Integer> buffer = new LinkedList<>();
        final int CAPACITY = 3;
        final Object lock = new Object();

        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 6; i++) {
                    synchronized (lock) {
                        while (buffer.size() == CAPACITY) {
                            System.out.println("[Producer] Buffer full (size=" + CAPACITY + "). Calling wait()...");
                            lock.wait();
                        }
                        buffer.add(i);
                        System.out.println("[Producer] Produced: " + i + " | Buffer: " + buffer);
                        lock.notifyAll();
                    }
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Producer");

        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < 6; i++) {
                    synchronized (lock) {
                        while (buffer.isEmpty()) {
                            System.out.println("[Consumer] Buffer empty. Calling wait()...");
                            lock.wait();
                        }
                        int item = buffer.poll();
                        System.out.println("[Consumer] Consumed: " + item + " | Buffer: " + buffer);
                        lock.notifyAll();
                    }
                    Thread.sleep(300);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Consumer");

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
        System.out.println("[Main] Producer-Consumer demo complete.");
    }

    // ─── DEMO 4: Lost Signal ─────────────────────────────────────────────

    private static void demoLostSignal() throws InterruptedException {
        final Object signal = new Object();

        System.out.println("Scenario: notify() fires BEFORE wait() — signal is lost.");
        System.out.println("(Using wait(2000) timeout so the demo doesn't hang forever.)\n");

        // Notifier runs FIRST — before the waiter starts
        Thread notifier = new Thread(() -> {
            synchronized (signal) {
                System.out.println("[Notifier] Calling notify()... but nobody is waiting yet!");
                signal.notify();
                System.out.println("[Notifier] notify() called. Signal is now LOST.");
            }
        }, "Notifier");

        notifier.start();
        notifier.join(); // ensure notifier completes first

        // Waiter runs SECOND — after the signal is already lost
        Thread waiter = new Thread(() -> {
            synchronized (signal) {
                System.out.println("[Waiter]   Calling wait(2000)...");
                long start = System.currentTimeMillis();
                try {
                    signal.wait(2000); // will timeout because the signal is already lost
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                long elapsed = System.currentTimeMillis() - start;
                System.out.println("[Waiter]   wait() returned after " + elapsed + "ms.");
                if (elapsed >= 1900) {
                    System.out.println("[Waiter]   Timed out! The notify() signal was LOST because it fired before wait().");
                    System.out.println("[Waiter]   FIX: Use a condition flag (boolean) checked in a while loop.");
                }
            }
        }, "Waiter");

        waiter.start();
        waiter.join();

        System.out.println("\nNow showing the FIX with a condition variable:");
        demoLostSignalFixed();
    }

    private static void demoLostSignalFixed() throws InterruptedException {
        final Object lock = new Object();
        final boolean[] ready = {false}; // condition variable

        Thread notifier = new Thread(() -> {
            synchronized (lock) {
                ready[0] = true; // set the condition BEFORE notify
                lock.notify();
                System.out.println("[Notifier-Fixed] Set ready=true and called notify().");
            }
        }, "Notifier-Fixed");

        notifier.start();
        notifier.join(); // notifier finishes first

        Thread waiter = new Thread(() -> {
            synchronized (lock) {
                // The while loop checks the flag — even though notify() already fired,
                // ready[0] is true, so we never enter wait() at all!
                while (!ready[0]) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                System.out.println("[Waiter-Fixed]   ready=" + ready[0] + " → Proceeding immediately. No lost signal!");
            }
        }, "Waiter-Fixed");

        waiter.start();
        waiter.join();
    }

    // ─── DEMO 5: Spurious Wakeup Protection ──────────────────────────────

    private static void demoSpuriousWakeupProtection() throws InterruptedException {
        final Object lock = new Object();
        final boolean[] dataReady = {false};
        final int[] wakeupCount = {0};

        Thread worker = new Thread(() -> {
            synchronized (lock) {
                System.out.println("[Worker] Waiting for data (using WHILE loop)...");
                while (!dataReady[0]) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    wakeupCount[0]++;
                    System.out.println("[Worker] Woke up (wakeup #" + wakeupCount[0]
                            + "). Checking condition: dataReady=" + dataReady[0]);
                    if (!dataReady[0]) {
                        System.out.println("[Worker] Condition still false → going back to wait (spurious or premature wakeup).");
                    }
                }
                System.out.println("[Worker] dataReady=true! Proceeding with work.");
                System.out.println("[Worker] Total wakeups before proceeding: " + wakeupCount[0]);
            }
        }, "Worker");

        worker.start();
        Thread.sleep(200);

        // First: send a "premature" notifyAll to simulate a spurious/premature wakeup
        synchronized (lock) {
            System.out.println("[Main] Sending notifyAll WITHOUT setting dataReady=true (simulating spurious wakeup)...");
            lock.notifyAll();
        }

        Thread.sleep(500);

        // Second: properly set the condition and notify
        synchronized (lock) {
            System.out.println("[Main] Now setting dataReady=true and calling notifyAll()...");
            dataReady[0] = true;
            lock.notifyAll();
        }

        worker.join();

        System.out.println("\n[Main] Key takeaway:");
        System.out.println("  - The WHILE loop protected against the premature/spurious wakeup.");
        System.out.println("  - Worker re-checked the condition and went back to waiting.");
        System.out.println("  - An IF statement would have let the worker proceed with dataReady=false!");
    }
}
