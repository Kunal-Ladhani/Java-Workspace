package com.scaler.concurrency.synchronization_deep_dive;

/**
 * Comprehensive demonstration of Java synchronization mechanisms.
 * Each section is self-contained and prints clear headers.
 */
public class SynchronizationDemo {

    // ──────────────────────────────────────────────────────────────
    // Section 1: Visibility issue without synchronization
    // ──────────────────────────────────────────────────────────────

    private static boolean running = true;

    private static void demoVisibilityIssue() throws InterruptedException {
        System.out.println("==========================================================");
        System.out.println("  DEMO 1: Visibility Issue Without Synchronization");
        System.out.println("==========================================================");
        System.out.println("A writer thread sets 'running = false', but the reader may");
        System.out.println("never see the update without volatile/synchronized.\n");

        running = true;

        Thread reader = new Thread(() -> {
            int iterations = 0;
            while (running) {
                iterations++;
            }
            System.out.println("[Reader] Stopped after " + iterations + " iterations.");
        });

        reader.start();
        Thread.sleep(100);

        System.out.println("[Writer] Setting running = false...");
        running = false;

        reader.join(2000);
        if (reader.isAlive()) {
            System.out.println("[Main]   Reader thread is STILL RUNNING — visibility issue!");
            System.out.println("[Main]   (The reader's cached copy of 'running' was never updated.)");
            System.out.println("[Main]   Interrupting reader to continue demo...");
            reader.interrupt();
            reader.join(500);
        } else {
            System.out.println("[Main]   Reader thread stopped (JVM happened to flush — not guaranteed).");
        }
        System.out.println();
    }

    // ──────────────────────────────────────────────────────────────
    // Section 2: Synchronized method vs synchronized block
    // ──────────────────────────────────────────────────────────────

    private int counterMethod = 0;
    private int counterBlock = 0;
    private final Object blockLock = new Object();

    public synchronized void incrementWithMethod() {
        counterMethod++;
    }

    public void incrementWithBlock() {
        synchronized (blockLock) {
            counterBlock++;
        }
    }

    private static void demoMethodVsBlock() throws InterruptedException {
        System.out.println("==========================================================");
        System.out.println("  DEMO 2: Synchronized Method vs Synchronized Block");
        System.out.println("==========================================================");

        SynchronizationDemo demo = new SynchronizationDemo();
        int iterations = 100_000;

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                demo.incrementWithMethod();
            }
        });
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                demo.incrementWithMethod();
            }
        });
        Thread t3 = new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                demo.incrementWithBlock();
            }
        });
        Thread t4 = new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                demo.incrementWithBlock();
            }
        });

        t1.start(); t2.start(); t3.start(); t4.start();
        t1.join(); t2.join(); t3.join(); t4.join();

        System.out.println("Synchronized METHOD counter: " + demo.counterMethod
                + " (expected: " + (2 * iterations) + ")");
        System.out.println("Synchronized BLOCK  counter: " + demo.counterBlock
                + " (expected: " + (2 * iterations) + ")");
        System.out.println("Both should match expected — proving mutual exclusion works.\n");
    }

    // ──────────────────────────────────────────────────────────────
    // Section 3: Static vs instance synchronization
    // ──────────────────────────────────────────────────────────────

    private static int staticCounter = 0;
    private int instanceCounter = 0;

    public static synchronized void incrementStatic() {
        staticCounter++;
    }

    public synchronized void incrementInstance() {
        instanceCounter++;
    }

    private static void demoStaticVsInstance() throws InterruptedException {
        System.out.println("==========================================================");
        System.out.println("  DEMO 3: Static vs Instance Synchronization");
        System.out.println("==========================================================");

        staticCounter = 0;
        SynchronizationDemo obj1 = new SynchronizationDemo();
        SynchronizationDemo obj2 = new SynchronizationDemo();
        int iterations = 100_000;

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                incrementStatic();
            }
        });
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                incrementStatic();
            }
        });

        Thread t3 = new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                obj1.incrementInstance();
            }
        });
        Thread t4 = new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                obj2.incrementInstance();
            }
        });

        t1.start(); t2.start(); t3.start(); t4.start();
        t1.join(); t2.join(); t3.join(); t4.join();

        System.out.println("Static synchronized counter: " + staticCounter
                + " (expected: " + (2 * iterations) + ")");
        System.out.println("  → Correct! Static sync locks on the Class object — one lock for all threads.\n");

        System.out.println("Instance counter (obj1): " + obj1.instanceCounter
                + " (expected: " + iterations + ")");
        System.out.println("Instance counter (obj2): " + obj2.instanceCounter
                + " (expected: " + iterations + ")");
        System.out.println("  → Each object has its own lock. Threads on different objects don't interfere.\n");
    }

    // ──────────────────────────────────────────────────────────────
    // Section 4: wait() / notify() pattern (Producer-Consumer)
    // ──────────────────────────────────────────────────────────────

    private static final Object monitor = new Object();
    private static boolean dataReady = false;
    private static String message = null;

    private static void demoWaitNotify() throws InterruptedException {
        System.out.println("==========================================================");
        System.out.println("  DEMO 4: wait() / notify() Pattern");
        System.out.println("==========================================================");
        System.out.println("A producer prepares data, a consumer waits for it.\n");

        dataReady = false;
        message = null;

        Thread consumer = new Thread(() -> {
            synchronized (monitor) {
                System.out.println("[Consumer] Waiting for data...");
                while (!dataReady) {
                    try {
                        monitor.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                System.out.println("[Consumer] Received: \"" + message + "\"");
            }
        });

        Thread producer = new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            synchronized (monitor) {
                message = "Hello from producer!";
                dataReady = true;
                System.out.println("[Producer] Data produced, notifying consumer...");
                monitor.notify();
            }
        });

        consumer.start();
        producer.start();

        consumer.join();
        producer.join();

        System.out.println("[Main]     wait()/notify() handoff completed successfully.\n");
    }

    // ──────────────────────────────────────────────────────────────
    // Section 5: Demonstrating unsynchronized counter (race condition preview)
    // ──────────────────────────────────────────────────────────────

    private static int unsafeCounter = 0;
    private static int safeCounter = 0;
    private static final Object counterLock = new Object();

    private static void demoUnsafeVsSafe() throws InterruptedException {
        System.out.println("==========================================================");
        System.out.println("  DEMO 5: Unsynchronized vs Synchronized Counter");
        System.out.println("==========================================================");

        unsafeCounter = 0;
        safeCounter = 0;
        int iterations = 200_000;

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                unsafeCounter++;
                synchronized (counterLock) { safeCounter++; }
            }
        });
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                unsafeCounter++;
                synchronized (counterLock) { safeCounter++; }
            }
        });

        t1.start(); t2.start();
        t1.join(); t2.join();

        int expected = 2 * iterations;
        System.out.println("Unsafe counter: " + unsafeCounter + " (expected: " + expected + ")");
        if (unsafeCounter != expected) {
            System.out.println("  → RACE CONDITION! Lost " + (expected - unsafeCounter) + " increments.");
        } else {
            System.out.println("  → Got lucky this run, but it's still unsafe.");
        }
        System.out.println("Safe counter:   " + safeCounter + " (expected: " + expected + ")");
        System.out.println("  → Always correct with synchronization.\n");
    }

    // ──────────────────────────────────────────────────────────────
    // Main
    // ──────────────────────────────────────────────────────────────

    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║         SYNCHRONIZATION DEEP DIVE — JAVA DEMO           ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");

        demoVisibilityIssue();
        demoMethodVsBlock();
        demoStaticVsInstance();
        demoWaitNotify();
        demoUnsafeVsSafe();

        System.out.println("==========================================================");
        System.out.println("  All demos completed.");
        System.out.println("==========================================================");
    }
}
