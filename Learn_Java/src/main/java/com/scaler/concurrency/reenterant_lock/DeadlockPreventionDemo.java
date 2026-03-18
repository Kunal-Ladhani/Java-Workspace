package com.scaler.concurrency.reenterant_lock;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Demonstrates how ReentrantLock's tryLock() prevents deadlocks.
 *
 * THE PROBLEM (Classic Deadlock):
 *   Thread-1: locks accountA, then tries to lock accountB
 *   Thread-2: locks accountB, then tries to lock accountA
 *   → Both threads wait forever. DEADLOCK.
 *
 * THE FIX:
 *   Use tryLock() to attempt acquiring the second lock.
 *   If it fails, RELEASE the first lock and retry.
 *   This breaks the "hold and wait" deadlock condition.
 *
 * Run this demo — it will always complete, never deadlock.
 */
public class DeadlockPreventionDemo {

    public static void main(String[] args) throws InterruptedException {
        BankAccount alice = new BankAccount("Alice", 1000);
        BankAccount bob = new BankAccount("Bob", 1000);

        System.out.println("Initial balances: " + alice + ", " + bob);
        System.out.println("Starting 100 concurrent transfers in both directions...\n");

        // Thread-1: transfers Alice → Bob (locks Alice first, then Bob)
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                transfer(alice, bob, 10);
            }
        }, "AliceToBob");

        // Thread-2: transfers Bob → Alice (locks Bob first, then Alice)
        // With regular lock(), this ordering would cause DEADLOCK.
        // With tryLock(), it gracefully retries.
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                transfer(bob, alice, 10);
            }
        }, "BobToAlice");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("\nAll transfers complete! No deadlock occurred.");
        System.out.println("Final balances: " + alice + ", " + bob);
        System.out.println("Total money in system: $" + (alice.getBalance() + bob.getBalance())
                + " (should be $2000 — no money lost)");
    }

    /**
     * Deadlock-safe transfer using tryLock().
     *
     * Strategy: try to lock both accounts. If we can't get BOTH,
     * release whatever we have and retry after a random backoff.
     */
    static void transfer(BankAccount from, BankAccount to, int amount) {
        int attempts = 0;

        while (true) {
            boolean gotFromLock = false;
            boolean gotToLock = false;

            try {
                gotFromLock = from.lock.tryLock(1, TimeUnit.MILLISECONDS);
                gotToLock = to.lock.tryLock(1, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            try {
                if (gotFromLock && gotToLock) {
                    // Got both locks — safe to transfer
                    if (from.getBalance() >= amount) {
                        from.debit(amount);
                        to.credit(amount);
                    }
                    return; // success!
                }
            } finally {
                if (gotFromLock) from.lock.unlock();
                if (gotToLock) to.lock.unlock();
            }

            // Couldn't get both locks — back off with random delay to reduce contention
            attempts++;
            if (attempts % 10 == 0) {
                System.out.println("  [" + Thread.currentThread().getName()
                        + "] Retried " + attempts + " times (lock contention). Backing off...");
            }
            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(1, 5));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    static class BankAccount {
        final String name;
        final ReentrantLock lock = new ReentrantLock();
        private int balance;

        BankAccount(String name, int balance) {
            this.name = name;
            this.balance = balance;
        }

        int getBalance() { return balance; }
        void debit(int amount) { balance -= amount; }
        void credit(int amount) { balance += amount; }

        @Override
        public String toString() {
            return name + "=$" + balance;
        }
    }
}
