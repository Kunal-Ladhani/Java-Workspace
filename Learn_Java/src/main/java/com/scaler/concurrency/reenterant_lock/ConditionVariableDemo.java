package com.scaler.concurrency.reenterant_lock;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Demonstrates Condition variables with ReentrantLock.
 *
 * This is the MOST POWERFUL feature that ReentrantLock has over synchronized.
 * With synchronized, you get ONE wait-set per monitor (wait/notify/notifyAll).
 * With ReentrantLock, you can create MULTIPLE Conditions, each with its own queue.
 *
 * This file implements a classic Bounded Buffer (Producer-Consumer) pattern
 * using two separate Conditions: notFull and notEmpty.
 *
 * Why two conditions?
 *   - Producers wait on "notFull"  → woken by consumers after they take an item.
 *   - Consumers wait on "notEmpty" → woken by producers after they put an item.
 *   - With synchronized, notifyAll() wakes ALL threads (wasteful).
 *   - With Conditions, signal() wakes ONLY the relevant thread.
 */
public class ConditionVariableDemo {

    public static void main(String[] args) throws InterruptedException {
        BoundedBuffer<String> buffer = new BoundedBuffer<>(3); // small buffer to see blocking

        // 2 Producers
        for (int i = 1; i <= 2; i++) {
            final int producerId = i;
            new Thread(() -> {
                try {
                    for (int j = 1; j <= 5; j++) {
                        String item = "P" + producerId + "-Item" + j;
                        buffer.put(item);
                        Thread.sleep(200); // simulate production time
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Producer-" + i).start();
        }

        // 2 Consumers (slower than producers to cause buffer-full situations)
        for (int i = 1; i <= 2; i++) {
            new Thread(() -> {
                try {
                    for (int j = 1; j <= 5; j++) {
                        buffer.take();
                        Thread.sleep(500); // simulate slow consumption
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Consumer-" + i).start();
        }
    }

    /**
     * A thread-safe bounded buffer using ReentrantLock + two Condition variables.
     *
     * Internal structure: circular array (ring buffer).
     *
     *   put()  → writes at putIndex, wraps around.
     *   take() → reads at takeIndex, wraps around.
     *
     *   notFull condition  → producers await here when buffer is full.
     *   notEmpty condition → consumers await here when buffer is empty.
     */
    static class BoundedBuffer<E> {

        private final ReentrantLock lock = new ReentrantLock();
        private final Condition notFull = lock.newCondition();
        private final Condition notEmpty = lock.newCondition();

        private final Object[] items;
        private int putIndex;
        private int takeIndex;
        private int count;

        public BoundedBuffer(int capacity) {
            items = new Object[capacity];
        }

        public void put(E item) throws InterruptedException {
            lock.lock();
            try {
                // MUST be while, not if — protects against spurious wakeups
                while (count == items.length) {
                    System.out.println("    [" + Thread.currentThread().getName()
                            + "] Buffer FULL (" + count + "/" + items.length + "). Waiting...");
                    notFull.await();  // release lock + sleep; re-acquire lock on wakeup
                }

                items[putIndex] = item;
                if (++putIndex == items.length) putIndex = 0; // wrap around
                count++;

                System.out.println("[" + Thread.currentThread().getName()
                        + "] PUT: " + item + "  (buffer: " + count + "/" + items.length + ")");

                notEmpty.signal(); // wake ONE consumer waiting on notEmpty
            } finally {
                lock.unlock();
            }
        }

        @SuppressWarnings("unchecked")
        public E take() throws InterruptedException {
            lock.lock();
            try {
                while (count == 0) {
                    System.out.println("    [" + Thread.currentThread().getName()
                            + "] Buffer EMPTY. Waiting...");
                    notEmpty.await();
                }

                E item = (E) items[takeIndex];
                items[takeIndex] = null; // help GC
                if (++takeIndex == items.length) takeIndex = 0;
                count--;

                System.out.println("[" + Thread.currentThread().getName()
                        + "] TAKE: " + item + " (buffer: " + count + "/" + items.length + ")");

                notFull.signal(); // wake ONE producer waiting on notFull
                return item;
            } finally {
                lock.unlock();
            }
        }
    }
}
