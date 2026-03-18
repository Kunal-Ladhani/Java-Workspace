package com.scaler.concurrency.read_write_lock;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Demonstrates ReentrantReadWriteLock features:
 *   1. Multiple concurrent readers (proved with timestamps)
 *   2. Writer blocks all readers (exclusive access)
 *   3. Thread-safe cache implementation using ReadWriteLock
 *   4. Lock downgrade (write lock → read lock → release write lock)
 *
 * Run main() and read the console output alongside the code to learn.
 */
public class ReadWriteLockDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========================================");
        System.out.println("  DEMO 1: Multiple Concurrent Readers");
        System.out.println("========================================");
        demoConcurrentReaders();

        Thread.sleep(1000);

        System.out.println("\n========================================");
        System.out.println("  DEMO 2: Writer Blocks All Readers");
        System.out.println("========================================");
        demoWriterBlocksReaders();

        Thread.sleep(1000);

        System.out.println("\n========================================");
        System.out.println("  DEMO 3: Thread-Safe Cache");
        System.out.println("========================================");
        demoThreadSafeCache();

        Thread.sleep(1000);

        System.out.println("\n========================================");
        System.out.println("  DEMO 4: Lock Downgrade");
        System.out.println("========================================");
        demoLockDowngrade();
    }

    // ──────────────────────────────────────────────
    //  DEMO 1: Multiple Concurrent Readers
    //  Multiple threads acquire the read lock simultaneously.
    //  Timestamps prove they overlap in time.
    // ──────────────────────────────────────────────
    private static void demoConcurrentReaders() throws InterruptedException {
        ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        long startTime = System.currentTimeMillis();

        Runnable readerTask = () -> {
            rwLock.readLock().lock();
            try {
                long elapsed = System.currentTimeMillis() - startTime;
                System.out.printf("  [%s] Acquired READ lock at +%dms%n",
                        Thread.currentThread().getName(), elapsed);
                Thread.sleep(500); // simulate reading for 500ms
                elapsed = System.currentTimeMillis() - startTime;
                System.out.printf("  [%s] Releasing READ lock at +%dms%n",
                        Thread.currentThread().getName(), elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                rwLock.readLock().unlock();
            }
        };

        Thread[] readers = new Thread[5];
        for (int i = 0; i < 5; i++) {
            readers[i] = new Thread(readerTask, "Reader-" + (i + 1));
        }
        for (Thread reader : readers) {
            reader.start();
            Thread.sleep(50); // stagger starts slightly
        }
        for (Thread reader : readers) {
            reader.join();
        }

        System.out.println("\n  ↑ Notice: All readers acquired the lock at nearly the same time (~+0ms to +200ms)");
        System.out.println("    and all released around the same time (~+500ms to +700ms).");
        System.out.println("    They ran CONCURRENTLY, not one after another!");
    }

    // ──────────────────────────────────────────────
    //  DEMO 2: Writer Blocks All Readers
    //  When a writer holds the write lock, readers must wait.
    //  When readers hold the read lock, writer must wait.
    // ──────────────────────────────────────────────
    private static void demoWriterBlocksReaders() throws InterruptedException {
        ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        long startTime = System.currentTimeMillis();

        Thread writer = new Thread(() -> {
            rwLock.writeLock().lock();
            try {
                long elapsed = System.currentTimeMillis() - startTime;
                System.out.printf("  [Writer] Acquired WRITE lock at +%dms — holding for 1 second%n", elapsed);
                Thread.sleep(1000);
                elapsed = System.currentTimeMillis() - startTime;
                System.out.printf("  [Writer] Releasing WRITE lock at +%dms%n", elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                rwLock.writeLock().unlock();
            }
        }, "Writer");

        Runnable readerTask = () -> {
            rwLock.readLock().lock();
            try {
                long elapsed = System.currentTimeMillis() - startTime;
                System.out.printf("  [%s] Acquired READ lock at +%dms%n",
                        Thread.currentThread().getName(), elapsed);
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                rwLock.readLock().unlock();
            }
        };

        writer.start();
        Thread.sleep(100); // ensure writer grabs lock first

        Thread[] readers = new Thread[3];
        for (int i = 0; i < 3; i++) {
            readers[i] = new Thread(readerTask, "Reader-" + (i + 1));
            readers[i].start();
        }

        writer.join();
        for (Thread reader : readers) {
            reader.join();
        }

        System.out.println("\n  ↑ Notice: Readers were BLOCKED until the writer released at ~+1000ms.");
        System.out.println("    The write lock is EXCLUSIVE — no reader can proceed while it's held.");
    }

    // ──────────────────────────────────────────────
    //  DEMO 3: Thread-Safe Cache
    //  A simple cache backed by HashMap + ReadWriteLock.
    //  Multiple readers access concurrently;
    //  writes are exclusive.
    // ──────────────────────────────────────────────
    private static void demoThreadSafeCache() throws InterruptedException {
        ReadWriteCache<String, Integer> cache = new ReadWriteCache<>();

        // Populate cache
        cache.put("alpha", 1);
        cache.put("beta", 2);
        cache.put("gamma", 3);
        System.out.println("  Cache populated: alpha=1, beta=2, gamma=3");

        // Concurrent readers
        Thread[] readers = new Thread[4];
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            readers[i] = new Thread(() -> {
                String[] keys = {"alpha", "beta", "gamma", "delta"};
                String key = keys[idx];
                Integer value = cache.get(key);
                System.out.printf("  [CacheReader-%d] get(\"%s\") = %s%n", idx + 1, key, value);
            }, "CacheReader-" + (i + 1));
        }

        // A concurrent writer
        Thread writer = new Thread(() -> {
            cache.put("delta", 4);
            System.out.println("  [CacheWriter] put(\"delta\", 4)");
        }, "CacheWriter");

        writer.start();
        for (Thread reader : readers) {
            reader.start();
        }

        writer.join();
        for (Thread reader : readers) {
            reader.join();
        }

        System.out.println("  Final cache size: " + cache.size());

        // computeIfAbsent demo
        Integer computed = cache.computeIfAbsent("epsilon", k -> {
            System.out.println("  [ComputeIfAbsent] Computing value for key: " + k);
            return k.length();
        });
        System.out.println("  computeIfAbsent(\"epsilon\") = " + computed);

        Integer cached = cache.computeIfAbsent("epsilon", k -> {
            System.out.println("  [ComputeIfAbsent] This should NOT print (value already cached)");
            return -1;
        });
        System.out.println("  computeIfAbsent(\"epsilon\") again = " + cached + " (from cache, no recompute)");
    }

    // ──────────────────────────────────────────────
    //  DEMO 4: Lock Downgrade
    //  Write lock → acquire read lock → release write lock → use read lock.
    //  This ensures no other writer can sneak in between the write and read.
    // ──────────────────────────────────────────────
    private static void demoLockDowngrade() throws InterruptedException {
        ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        final long[] sharedData = {0};

        Thread downgrader = new Thread(() -> {
            // Step 1: Acquire write lock
            rwLock.writeLock().lock();
            System.out.println("  [Downgrader] Acquired WRITE lock");
            System.out.println("  [Downgrader] isWriteLocked = " + rwLock.isWriteLocked());
            System.out.println("  [Downgrader] readLockCount = " + rwLock.getReadLockCount());

            try {
                // Step 2: Perform the write
                sharedData[0] = 42;
                System.out.println("  [Downgrader] Wrote value: " + sharedData[0]);

                // Step 3: Downgrade — acquire read lock WHILE holding write lock
                rwLock.readLock().lock();
                System.out.println("  [Downgrader] Acquired READ lock (while holding write)");
                System.out.println("  [Downgrader] isWriteLocked = " + rwLock.isWriteLocked());
                System.out.println("  [Downgrader] readLockCount = " + rwLock.getReadLockCount());
            } finally {
                // Step 4: Release write lock — now only holding read lock
                rwLock.writeLock().unlock();
            }

            System.out.println("  [Downgrader] Released WRITE lock — now only holding READ lock");
            System.out.println("  [Downgrader] isWriteLocked = " + rwLock.isWriteLocked());
            System.out.println("  [Downgrader] readLockCount = " + rwLock.getReadLockCount());

            try {
                // Step 5: Continue reading under read lock
                System.out.println("  [Downgrader] Reading value under READ lock: " + sharedData[0]);
                Thread.sleep(500); // hold read lock so other readers can join
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                rwLock.readLock().unlock();
                System.out.println("  [Downgrader] Released READ lock");
            }
        }, "Downgrader");

        // A reader that will be able to join once write lock is released
        Thread reader = new Thread(() -> {
            try {
                Thread.sleep(200); // wait for downgrader to start
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("  [Reader] Attempting to acquire READ lock...");
            rwLock.readLock().lock();
            try {
                System.out.println("  [Reader] Acquired READ lock — value = " + sharedData[0]);
                System.out.println("  [Reader] Running concurrently with downgrader's read phase!");
            } finally {
                rwLock.readLock().unlock();
                System.out.println("  [Reader] Released READ lock");
            }
        }, "Reader");

        // A writer that must wait until all read locks are released
        Thread writer = new Thread(() -> {
            try {
                Thread.sleep(300); // wait for downgrade to happen
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("  [Writer] Attempting to acquire WRITE lock...");
            rwLock.writeLock().lock();
            try {
                System.out.println("  [Writer] Acquired WRITE lock — all readers are done");
                sharedData[0] = 99;
                System.out.println("  [Writer] Wrote value: " + sharedData[0]);
            } finally {
                rwLock.writeLock().unlock();
                System.out.println("  [Writer] Released WRITE lock");
            }
        }, "Writer");

        downgrader.start();
        reader.start();
        writer.start();

        downgrader.join();
        reader.join();
        writer.join();

        System.out.println("\n  ↑ Notice the sequence:");
        System.out.println("    1. Downgrader holds WRITE lock exclusively");
        System.out.println("    2. Downgrader acquires READ lock, then releases WRITE lock (downgrade)");
        System.out.println("    3. Reader joins — runs concurrently with downgrader's read phase");
        System.out.println("    4. Writer waits until both readers finish, then acquires WRITE lock");
    }

    // ──────────────────────────────────────────────
    //  Thread-safe cache using ReadWriteLock
    // ──────────────────────────────────────────────
    static class ReadWriteCache<K, V> {
        private final Map<K, V> map = new HashMap<>();
        private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

        public V get(K key) {
            rwLock.readLock().lock();
            try {
                return map.get(key);
            } finally {
                rwLock.readLock().unlock();
            }
        }

        public void put(K key, V value) {
            rwLock.writeLock().lock();
            try {
                map.put(key, value);
            } finally {
                rwLock.writeLock().unlock();
            }
        }

        public V computeIfAbsent(K key, java.util.function.Function<K, V> mappingFunction) {
            rwLock.readLock().lock();
            try {
                V value = map.get(key);
                if (value != null) return value;
            } finally {
                rwLock.readLock().unlock();
            }
            rwLock.writeLock().lock();
            try {
                V value = map.get(key);
                if (value == null) {
                    value = mappingFunction.apply(key);
                    map.put(key, value);
                }
                return value;
            } finally {
                rwLock.writeLock().unlock();
            }
        }

        public int size() {
            rwLock.readLock().lock();
            try {
                return map.size();
            } finally {
                rwLock.readLock().unlock();
            }
        }
    }
}
