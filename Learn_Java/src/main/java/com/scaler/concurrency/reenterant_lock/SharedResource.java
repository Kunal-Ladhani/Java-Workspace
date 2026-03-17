package com.scaler.concurrency.reenterant_lock;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SharedResource {

    private static boolean isAvailable = true;
    private static Lock lock = new ReentrantLock();

    public void produce() {

        try {
            lock.lock();

            System.out.println(Thread.currentThread().getName() + " STARTS with critical section.");
            isAvailable = false;
            Thread.sleep(10_000L);
            System.out.println(Thread.currentThread().getName() + " ENDS with critical section.");

        } catch (InterruptedException e) {
            // log the interrupted exception if it happens
        } finally {
            isAvailable = true;
            lock.unlock();
        }

    }

}
