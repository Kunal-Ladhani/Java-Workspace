package com.scaler.concurrency.bank_transfer_problem;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BankStressTest {

    @Test
    void stressTestTransfers() throws Exception {

        int threadCount = 100;
        int transferAmount = 10;

        Account a = new Account(1, 100000);
        Account b = new Account(2, 100000);

        int initialTotal = a.getBalance() + b.getBalance();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);

        IntStream.range(0, threadCount).forEach(i -> {

            executor.execute(() -> {

                ready.countDown();

                try {
                    start.await(); // wait for simultaneous start

                    BankService.transfer(a, b, transferAmount);

                } catch (InterruptedException ignored) {
                }

                done.countDown();
            });

        });

        ready.await(); // wait for all threads ready
        start.countDown(); // start all threads

        done.await(); // wait for completion

        executor.shutdown();

        int finalTotal = a.getBalance() + b.getBalance();

        assertEquals(initialTotal, finalTotal);
    }
}
