package com.scaler.concurrency.bank_transfer_problem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BankConcurrencyTest {

    @Test
    void testConcurrentTransfers() throws Exception {

        Account a = new Account(1, 10000);
        Account b = new Account(2, 10000);

        Thread[] threads = new Thread[10];

        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() ->
                    BankService.transfer(a, b, 100)
            );
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        assertEquals(9000, a.getBalance());
        assertEquals(11000, b.getBalance());
    }

    @Test
    void testDeadlockScenario() throws Exception {

        Account a = new Account(1, 1000);
        Account b = new Account(2, 1000);

        Thread t1 = new Thread(() ->
                BankService.transfer(a, b, 100));

        Thread t2 = new Thread(() ->
                BankService.transfer(b, a, 200));

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        int total = a.getBalance() + b.getBalance();

        assertEquals(2000, total);
    }

    @Test
    void testTotalBalanceInvariant() throws Exception {

        Account a = new Account(1, 5000);
        Account b = new Account(2, 5000);

        int initialTotal = a.getBalance() + b.getBalance();

        Thread[] threads = new Thread[20];

        for (int i = 0; i < 20; i++) {
            threads[i] = new Thread(() ->
                    BankService.transfer(a, b, 50)
            );
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        int finalTotal = a.getBalance() + b.getBalance();

        assertEquals(initialTotal, finalTotal);
    }
}