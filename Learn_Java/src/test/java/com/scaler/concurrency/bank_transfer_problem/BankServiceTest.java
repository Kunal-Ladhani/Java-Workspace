package com.scaler.concurrency.bank_transfer_problem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BankServiceTest {

    @Test
    void testBasicTransfer() {

        Account a = new Account(1, 1000);
        Account b = new Account(2, 500);

        BankService.transfer(a, b, 200);

        assertEquals(800, a.getBalance());
        assertEquals(700, b.getBalance());
    }

    @Test
    void testMultipleSequentialTransfers() {

        Account a = new Account(1, 1000);
        Account b = new Account(2, 1000);

        for (int i = 0; i < 5; i++) {
            BankService.transfer(a, b, 100);
        }

        assertEquals(500, a.getBalance());
        assertEquals(1500, b.getBalance());
    }

    @Test
    void testSelfTransferDoesNothing() {

        Account a = new Account(1, 1000);

        BankService.transfer(a, a, 200);

        assertEquals(1000, a.getBalance());
    }

    @Test
    void testInsufficientBalance() {

        Account a = new Account(1, 100);
        Account b = new Account(2, 1000);

        BankService.transfer(a, b, 500);

        assertEquals(100, a.getBalance());
        assertEquals(1000, b.getBalance());
    }

}