package com.scaler.concurrency.stamped_lock;

import java.util.concurrent.locks.StampedLock;

/**
 * Demonstrates StampedLock features:
 *   1. Basic write lock and read lock with stamps
 *   2. Optimistic read pattern (Point class with x,y coordinates)
 *   3. Lock conversion: optimistic read → read lock → write lock
 *   4. Performance comparison: optimistic read vs regular read lock
 *
 * Run main() and read the console output alongside the code to learn.
 */
public class StampedLockDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========================================");
        System.out.println("  DEMO 1: Basic Write & Read Locks");
        System.out.println("========================================");
        demoBasicLocks();

        Thread.sleep(1000);

        System.out.println("\n========================================");
        System.out.println("  DEMO 2: Optimistic Read Pattern");
        System.out.println("========================================");
        demoOptimisticRead();

        Thread.sleep(1000);

        System.out.println("\n========================================");
        System.out.println("  DEMO 3: Lock Conversion");
        System.out.println("========================================");
        demoLockConversion();

        Thread.sleep(500);

        System.out.println("\n========================================");
        System.out.println("  DEMO 4: Performance Comparison");
        System.out.println("========================================");
        demoPerformanceComparison();
    }

    // ──────────────────────────────────────────────
    //  DEMO 1: Basic Write & Read Locks with Stamps
    //  Every lock acquisition returns a stamp.
    //  The stamp must be passed to unlock.
    // ──────────────────────────────────────────────
    private static void demoBasicLocks() throws InterruptedException {
        StampedLock sl = new StampedLock();
        long startTime = System.currentTimeMillis();

        Thread writer = new Thread(() -> {
            long stamp = sl.writeLock();
            try {
                long elapsed = System.currentTimeMillis() - startTime;
                System.out.printf("  [Writer] Acquired WRITE lock (stamp=%d) at +%dms%n", stamp, elapsed);
                System.out.println("  [Writer] isWriteLocked = " + sl.isWriteLocked());
                Thread.sleep(500);
                elapsed = System.currentTimeMillis() - startTime;
                System.out.printf("  [Writer] Releasing WRITE lock at +%dms%n", elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                sl.unlockWrite(stamp);
            }
        }, "Writer");

        Runnable readerTask = () -> {
            long stamp = sl.readLock();
            try {
                long elapsed = System.currentTimeMillis() - startTime;
                System.out.printf("  [%s] Acquired READ lock (stamp=%d) at +%dms%n",
                        Thread.currentThread().getName(), stamp, elapsed);
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                sl.unlockRead(stamp);
            }
        };

        writer.start();
        Thread.sleep(50);

        Thread[] readers = new Thread[3];
        for (int i = 0; i < 3; i++) {
            readers[i] = new Thread(readerTask, "Reader-" + (i + 1));
            readers[i].start();
        }

        writer.join();
        for (Thread r : readers) {
            r.join();
        }

        System.out.println("\n  ↑ Readers were blocked until writer released.");
        System.out.println("    After writer released, all readers acquired simultaneously.");
    }

    // ──────────────────────────────────────────────
    //  DEMO 2: Optimistic Read Pattern
    //  A thread-safe Point class using the canonical
    //  optimistic read pattern from the StampedLock Javadoc.
    // ──────────────────────────────────────────────
    private static void demoOptimisticRead() throws InterruptedException {
        Point point = new Point(3.0, 4.0);
        System.out.println("  Initial point: (" + point.getX() + ", " + point.getY() + ")");
        System.out.println("  Initial distance from origin: " + point.distanceFromOrigin());

        System.out.println("\n  --- Optimistic reads with NO concurrent writes ---");
        for (int i = 0; i < 3; i++) {
            double dist = point.distanceFromOrigin();
            System.out.printf("  Read %d: distance = %.2f (optimistic read likely succeeded)%n", i + 1, dist);
        }

        System.out.println("\n  --- Optimistic reads WITH concurrent writes ---");
        Thread writer = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                point.move(0.5, 0.5);
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "PointWriter");

        Thread reader = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                double dist = point.distanceFromOrigin();
                System.out.printf("  [PointReader] distance = %.4f%n", dist);
                try {
                    Thread.sleep(15);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "PointReader");

        writer.start();
        reader.start();
        writer.join();
        reader.join();

        System.out.printf("%n  Final point after moves: (%.1f, %.1f)%n", point.getX(), point.getY());
        System.out.printf("  Final distance: %.4f%n", point.distanceFromOrigin());
    }

    // ──────────────────────────────────────────────
    //  DEMO 3: Lock Conversion
    //  Shows conversion between lock modes:
    //   - optimistic read → read lock (when validation fails)
    //   - read lock → write lock (upgrade)
    //   - write lock → read lock (downgrade)
    // ──────────────────────────────────────────────
    private static void demoLockConversion() throws InterruptedException {
        StampedLock sl = new StampedLock();
        final double[] data = {10.0};

        System.out.println("  --- Conversion: Read Lock → Write Lock (upgrade) ---");

        Thread upgrader = new Thread(() -> {
            long stamp = sl.readLock();
            System.out.println("  [Upgrader] Acquired READ lock (stamp=" + stamp + ")");

            try {
                double value = data[0];
                System.out.println("  [Upgrader] Read value: " + value);

                if (value < 100.0) {
                    System.out.println("  [Upgrader] Value too low — attempting upgrade to WRITE lock...");
                    long ws = sl.tryConvertToWriteLock(stamp);

                    if (ws != 0L) {
                        stamp = ws;
                        System.out.println("  [Upgrader] Upgrade SUCCEEDED (new stamp=" + stamp + ")");
                    } else {
                        System.out.println("  [Upgrader] Upgrade FAILED — releasing read, acquiring write...");
                        sl.unlockRead(stamp);
                        stamp = sl.writeLock();
                        System.out.println("  [Upgrader] Acquired WRITE lock the hard way (stamp=" + stamp + ")");
                    }

                    data[0] = 100.0;
                    System.out.println("  [Upgrader] Wrote new value: " + data[0]);
                }
            } finally {
                sl.unlock(stamp);
                System.out.println("  [Upgrader] Released lock");
            }
        }, "Upgrader");

        upgrader.start();
        upgrader.join();

        System.out.println("\n  --- Conversion: Write Lock → Read Lock (downgrade) ---");

        Thread downgrader = new Thread(() -> {
            long stamp = sl.writeLock();
            System.out.println("  [Downgrader] Acquired WRITE lock (stamp=" + stamp + ")");
            System.out.println("  [Downgrader] isWriteLocked = " + sl.isWriteLocked());

            data[0] = 200.0;
            System.out.println("  [Downgrader] Wrote value: " + data[0]);

            long readStamp = sl.tryConvertToReadLock(stamp);
            if (readStamp != 0L) {
                stamp = readStamp;
                System.out.println("  [Downgrader] Downgraded to READ lock (new stamp=" + stamp + ")");
                System.out.println("  [Downgrader] isWriteLocked = " + sl.isWriteLocked());
                System.out.println("  [Downgrader] isReadLocked  = " + sl.isReadLocked());
                System.out.println("  [Downgrader] Reading value under read lock: " + data[0]);
            }

            sl.unlock(stamp);
            System.out.println("  [Downgrader] Released lock");
        }, "Downgrader");

        downgrader.start();
        downgrader.join();

        System.out.println("\n  --- Conversion: Optimistic Read → Read Lock (on validation failure) ---");

        long writeStamp = sl.writeLock();
        System.out.println("  [Main] Holding WRITE lock to force optimistic read failure");

        Thread optimisticReader = new Thread(() -> {
            long stamp = sl.tryOptimisticRead();
            System.out.println("  [OptReader] Got optimistic stamp: " + stamp);

            double value = data[0];

            boolean valid = sl.validate(stamp);
            System.out.println("  [OptReader] validate() = " + valid);

            if (!valid) {
                System.out.println("  [OptReader] Optimistic read failed — converting to read lock...");
                long rs = sl.readLock();
                try {
                    value = data[0];
                    System.out.println("  [OptReader] Re-read under READ lock: " + value);
                } finally {
                    sl.unlockRead(rs);
                }
            } else {
                System.out.println("  [OptReader] Optimistic read succeeded: " + value);
            }
        }, "OptReader");

        optimisticReader.start();
        Thread.sleep(200); // let the optimistic reader try and block on readLock

        data[0] = 300.0;
        sl.unlockWrite(writeStamp);
        System.out.println("  [Main] Released WRITE lock");

        optimisticReader.join();
    }

    // ──────────────────────────────────────────────
    //  DEMO 4: Performance Comparison
    //  Optimistic read vs pessimistic read lock
    //  under low contention (single-threaded reads).
    // ──────────────────────────────────────────────
    private static void demoPerformanceComparison() {
        StampedLock sl = new StampedLock();
        final double[] data = {42.0, 84.0};
        final int ITERATIONS = 5_000_000;

        // Warm up
        for (int i = 0; i < 100_000; i++) {
            long stamp = sl.tryOptimisticRead();
            double d = data[0] + data[1];
            sl.validate(stamp);
            stamp = sl.readLock();
            d = data[0] + data[1];
            sl.unlockRead(stamp);
        }

        // Benchmark: Optimistic Read
        long start = System.nanoTime();
        double sum = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            long stamp = sl.tryOptimisticRead();
            double d0 = data[0];
            double d1 = data[1];
            if (!sl.validate(stamp)) {
                stamp = sl.readLock();
                try {
                    d0 = data[0];
                    d1 = data[1];
                } finally {
                    sl.unlockRead(stamp);
                }
            }
            sum += d0 + d1;
        }
        long optimisticTime = System.nanoTime() - start;

        // Benchmark: Pessimistic Read Lock
        start = System.nanoTime();
        double sum2 = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            long stamp = sl.readLock();
            try {
                sum2 += data[0] + data[1];
            } finally {
                sl.unlockRead(stamp);
            }
        }
        long pessimisticTime = System.nanoTime() - start;

        System.out.printf("  Iterations:     %,d%n", ITERATIONS);
        System.out.printf("  Optimistic Read: %,d ms  (sum=%.0f)%n", optimisticTime / 1_000_000, sum);
        System.out.printf("  Pessimistic Read: %,d ms  (sum=%.0f)%n", pessimisticTime / 1_000_000, sum2);

        double speedup = (double) pessimisticTime / optimisticTime;
        System.out.printf("  Speedup: %.2fx faster with optimistic reads%n", speedup);

        System.out.println("\n  ↑ Under zero contention (no concurrent writers),");
        System.out.println("    optimistic reads avoid the CAS overhead of readLock().");
        System.out.println("    The difference grows with more reader threads (less cache-line bouncing).");

        if (speedup < 1.0) {
            System.out.println("\n  Note: JVM optimizations may narrow the gap in microbenchmarks.");
            System.out.println("  The real benefit shows under multi-threaded read contention.");
        }
    }

    // ──────────────────────────────────────────────
    //  Thread-safe Point class using StampedLock
    //  with optimistic read pattern
    // ──────────────────────────────────────────────
    static class Point {
        private double x;
        private double y;
        private final StampedLock sl = new StampedLock();

        Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        void move(double deltaX, double deltaY) {
            long stamp = sl.writeLock();
            try {
                x += deltaX;
                y += deltaY;
            } finally {
                sl.unlockWrite(stamp);
            }
        }

        double distanceFromOrigin() {
            long stamp = sl.tryOptimisticRead();
            double currentX = x;
            double currentY = y;

            if (!sl.validate(stamp)) {
                stamp = sl.readLock();
                try {
                    currentX = x;
                    currentY = y;
                } finally {
                    sl.unlockRead(stamp);
                }
            }

            return Math.sqrt(currentX * currentX + currentY * currentY);
        }

        double getX() {
            long stamp = sl.tryOptimisticRead();
            double lx = x;
            if (!sl.validate(stamp)) {
                stamp = sl.readLock();
                try { lx = x; } finally { sl.unlockRead(stamp); }
            }
            return lx;
        }

        double getY() {
            long stamp = sl.tryOptimisticRead();
            double ly = y;
            if (!sl.validate(stamp)) {
                stamp = sl.readLock();
                try { ly = y; } finally { sl.unlockRead(stamp); }
            }
            return ly;
        }
    }
}
