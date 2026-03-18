package com.scaler.concurrency.semaphore;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class SemaphoreDemo {

    // ==================== DEMO 1: Connection Pool Simulator ====================

    static class ConnectionPool {
        private final Semaphore semaphore;
        private final String[] connections;
        private final boolean[] inUse;

        ConnectionPool(int size) {
            this.semaphore = new Semaphore(size, true);
            this.connections = new String[size];
            this.inUse = new boolean[size];
            for (int i = 0; i < size; i++) {
                connections[i] = "Connection-" + (i + 1);
            }
        }

        String acquire() throws InterruptedException {
            semaphore.acquire();
            return getNextAvailable();
        }

        void release(String connection) {
            if (markAsUnused(connection)) {
                semaphore.release();
            }
        }

        private synchronized String getNextAvailable() {
            for (int i = 0; i < connections.length; i++) {
                if (!inUse[i]) {
                    inUse[i] = true;
                    return connections[i];
                }
            }
            return null;
        }

        private synchronized boolean markAsUnused(String connection) {
            for (int i = 0; i < connections.length; i++) {
                if (connections[i].equals(connection)) {
                    inUse[i] = false;
                    return true;
                }
            }
            return false;
        }
    }

    private static void demoConnectionPool() throws InterruptedException {
        System.out.println("=".repeat(70));
        System.out.println("DEMO 1: Connection Pool Simulator");
        System.out.println("  10 threads competing for 3 database connections");
        System.out.println("=".repeat(70));

        ConnectionPool pool = new ConnectionPool(3);
        Thread[] threads = new Thread[10];

        for (int i = 0; i < 10; i++) {
            final int threadId = i + 1;
            threads[i] = new Thread(() -> {
                try {
                    System.out.printf("  [Thread-%02d] Waiting to acquire connection...%n", threadId);
                    String conn = pool.acquire();
                    System.out.printf("  [Thread-%02d] ✓ Acquired %s%n", threadId, conn);

                    Thread.sleep((long) (Math.random() * 500 + 200));

                    pool.release(conn);
                    System.out.printf("  [Thread-%02d] Released %s%n", threadId, conn);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        for (Thread t : threads) {
            t.start();
            Thread.sleep(50);
        }
        for (Thread t : threads) {
            t.join();
        }

        System.out.println("\n  All threads completed. Connection pool demo finished.\n");
    }

    // ==================== DEMO 2: Rate Limiter ====================

    private static void demoRateLimiter() throws InterruptedException {
        System.out.println("=".repeat(70));
        System.out.println("DEMO 2: Rate Limiter — Max 2 Concurrent API Calls");
        System.out.println("=".repeat(70));

        Semaphore rateLimiter = new Semaphore(2);
        Thread[] threads = new Thread[6];

        for (int i = 0; i < 6; i++) {
            final int requestId = i + 1;
            threads[i] = new Thread(() -> {
                try {
                    System.out.printf("  [Request-%d] Attempting API call... (available permits: %d)%n",
                            requestId, rateLimiter.availablePermits());

                    rateLimiter.acquire();
                    System.out.printf("  [Request-%d] ✓ API call started (permits remaining: %d)%n",
                            requestId, rateLimiter.availablePermits());

                    Thread.sleep(800);

                    rateLimiter.release();
                    System.out.printf("  [Request-%d] API call completed, permit released%n", requestId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        for (Thread t : threads) {
            t.start();
            Thread.sleep(100);
        }
        for (Thread t : threads) {
            t.join();
        }

        System.out.println("\n  All API calls completed. Rate limiter demo finished.\n");
    }

    // ==================== DEMO 3: Binary Semaphore for Signaling ====================

    private static void demoBinarySignaling() throws InterruptedException {
        System.out.println("=".repeat(70));
        System.out.println("DEMO 3: Binary Semaphore as a Signaling Mechanism");
        System.out.println("  Thread-B signals Thread-A when data is ready");
        System.out.println("=".repeat(70));

        Semaphore signal = new Semaphore(0);
        final String[] sharedData = {null};

        Thread threadA = new Thread(() -> {
            try {
                System.out.println("  [Thread-A] Waiting for data to be prepared...");
                signal.acquire();
                System.out.println("  [Thread-A] ✓ Signal received! Data = \"" + sharedData[0] + "\"");
                System.out.println("  [Thread-A] Processing data...");
                Thread.sleep(200);
                System.out.println("  [Thread-A] Done processing.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Thread-A");

        Thread threadB = new Thread(() -> {
            try {
                System.out.println("  [Thread-B] Preparing data...");
                Thread.sleep(500);
                sharedData[0] = "Hello from Thread-B!";
                System.out.println("  [Thread-B] Data ready. Sending signal to Thread-A...");
                signal.release();
                System.out.println("  [Thread-B] ✓ Signal sent (release called by non-acquiring thread).");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Thread-B");

        threadA.start();
        Thread.sleep(100);
        threadB.start();

        threadA.join();
        threadB.join();

        System.out.println("\n  Key insight: Thread-B called release() without ever calling acquire().");
        System.out.println("  This is legal because semaphores have NO ownership.\n");
    }

    // ==================== DEMO 4: tryAcquire() Non-Blocking ====================

    private static void demoTryAcquire() throws InterruptedException {
        System.out.println("=".repeat(70));
        System.out.println("DEMO 4: tryAcquire() — Non-Blocking Permit Acquisition");
        System.out.println("=".repeat(70));

        Semaphore sem = new Semaphore(1);

        Thread holder = new Thread(() -> {
            try {
                sem.acquire();
                System.out.println("  [Holder]  Acquired permit, holding for 1 second...");
                Thread.sleep(1000);
                sem.release();
                System.out.println("  [Holder]  Released permit.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        holder.start();
        Thread.sleep(100);

        System.out.println("  [Main]    Trying tryAcquire() (immediate)...");
        boolean acquired = sem.tryAcquire();
        System.out.println("  [Main]    tryAcquire() returned: " + acquired + " (expected: false)");

        System.out.println("  [Main]    Trying tryAcquire(500ms)...");
        acquired = sem.tryAcquire(500, TimeUnit.MILLISECONDS);
        System.out.println("  [Main]    tryAcquire(500ms) returned: " + acquired + " (expected: false — timed out)");

        System.out.println("  [Main]    Trying tryAcquire(2s) — should succeed after holder releases...");
        acquired = sem.tryAcquire(2, TimeUnit.SECONDS);
        System.out.println("  [Main]    tryAcquire(2s) returned: " + acquired + " (expected: true)");
        if (acquired) {
            sem.release();
        }

        holder.join();
        System.out.println("\n  tryAcquire() demo finished.\n");
    }

    // ==================== DEMO 5: Permits Can Exceed Initial Count ====================

    private static void demoPermitOverflow() {
        System.out.println("=".repeat(70));
        System.out.println("DEMO 5: Permits Can Exceed Initial Count (Common Pitfall!)");
        System.out.println("=".repeat(70));

        Semaphore sem = new Semaphore(3);
        System.out.println("  Initial permits: " + sem.availablePermits());

        sem.release();
        System.out.println("  After extra release(): " + sem.availablePermits() + "  ← Exceeded initial count!");

        sem.release(5);
        System.out.println("  After release(5):      " + sem.availablePermits() + "  ← Way beyond initial count!");

        System.out.println("\n  WARNING: Java Semaphore does NOT enforce an upper bound.");
        System.out.println("  Unmatched release() calls silently inflate the permit count.\n");
    }

    // ==================== MAIN ====================

    public static void main(String[] args) throws InterruptedException {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║              SEMAPHORE — Comprehensive Demo Suite                    ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════╝");
        System.out.println();

        demoConnectionPool();
        demoRateLimiter();
        demoBinarySignaling();
        demoTryAcquire();
        demoPermitOverflow();

        System.out.println("=".repeat(70));
        System.out.println("All demos completed successfully!");
        System.out.println("=".repeat(70));
    }
}
