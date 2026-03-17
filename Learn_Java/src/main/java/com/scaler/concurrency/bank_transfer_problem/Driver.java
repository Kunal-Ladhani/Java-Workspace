package com.scaler.concurrency.bank_transfer_problem;

public class Driver {

    public static void main(String[] args) throws Exception {

        test1_basicTransfer();
        test2_multipleSequentialTransfers();
        test3_bidirectionalTransferDeadlockCase();
        test4_concurrentTransfers();
        test5_highContention();
        test6_selfTransfer();
        test7_insufficientBalance();
        test8_manyAccountsRandomTransfers();
        test9_largeTransfer();
        test10_totalBalanceInvariant();
    }

    // ------------------------------------------------

    static void test1_basicTransfer() {

        System.out.println("\nTEST 1 : Basic Transfer");

        Account a = new Account(1, 1000);
        Account b = new Account(2, 500);

        BankService.transfer(a, b, 200);

        System.out.println("A = " + a.getBalance()); // 800
        System.out.println("B = " + b.getBalance()); // 700
    }

    // ------------------------------------------------

    static void test2_multipleSequentialTransfers() {

        System.out.println("\nTEST 2 : Multiple Sequential Transfers");

        Account a = new Account(1, 1000);
        Account b = new Account(2, 1000);

        for (int i = 0; i < 5; i++) {
            BankService.transfer(a, b, 100);
        }

        System.out.println("A = " + a.getBalance()); // 500
        System.out.println("B = " + b.getBalance()); // 1500
    }

    // ------------------------------------------------

    static void test3_bidirectionalTransferDeadlockCase() throws Exception {

        System.out.println("\nTEST 3 : Deadlock Scenario Test");

        Account a = new Account(1, 1000);
        Account b = new Account(2, 1000);

        Thread t1 = new Thread(() -> BankService.transfer(a, b, 100));
        Thread t2 = new Thread(() -> BankService.transfer(b, a, 200));

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println("Completed without deadlock");
        System.out.println("A = " + a.getBalance());
        System.out.println("B = " + b.getBalance());
    }

    // ------------------------------------------------

    static void test4_concurrentTransfers() throws Exception {

        System.out.println("\nTEST 4 : Concurrent Transfers");

        Account a = new Account(1, 10000);
        Account b = new Account(2, 10000);

        Thread[] threads = new Thread[10];

        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> BankService.transfer(a, b, 100));
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        System.out.println("A = " + a.getBalance()); // 9000
        System.out.println("B = " + b.getBalance()); // 11000
    }

    // ------------------------------------------------

    static void test5_highContention() throws Exception {

        System.out.println("\nTEST 5 : High Contention (50 threads)");

        Account a = new Account(1, 10000);
        Account b = new Account(2, 10000);

        Thread[] threads = new Thread[50];

        for (int i = 0; i < 50; i++) {
            threads[i] = new Thread(() -> BankService.transfer(a, b, 10));
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        System.out.println("A = " + a.getBalance());
        System.out.println("B = " + b.getBalance());
    }

    // ------------------------------------------------

    static void test6_selfTransfer() {

        System.out.println("\nTEST 6 : Self Transfer");

        Account a = new Account(1, 1000);

        BankService.transfer(a, a, 200);

        System.out.println("A = " + a.getBalance()); // should stay 1000
    }

    // ------------------------------------------------

    static void test7_insufficientBalance() {

        System.out.println("\nTEST 7 : Insufficient Balance");

        Account a = new Account(1, 100);
        Account b = new Account(2, 1000);

        BankService.transfer(a, b, 500);

        System.out.println("A = " + a.getBalance()); // 100
        System.out.println("B = " + b.getBalance()); // 1000
    }

    // ------------------------------------------------

    static void test8_manyAccountsRandomTransfers() throws Exception {

        System.out.println("\nTEST 8 : Random Transfers Between Accounts");

        Account[] accounts = {
                new Account(1, 1000),
                new Account(2, 1000),
                new Account(3, 1000),
                new Account(4, 1000)
        };

        Thread[] threads = new Thread[20];

        for (int i = 0; i < 20; i++) {

            threads[i] = new Thread(() -> {

                int from = (int) (Math.random() * 4);
                int to = (int) (Math.random() * 4);

                BankService.transfer(accounts[from], accounts[to], 50);

            });

        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        System.out.println("Random transfers completed");
    }

    // ------------------------------------------------

    static void test9_largeTransfer() {

        System.out.println("\nTEST 9 : Large Transfer");

        Account a = new Account(1, 1_000_000);
        Account b = new Account(2, 1_000_000);

        BankService.transfer(a, b, 500000);

        System.out.println("A = " + a.getBalance()); // 500000
        System.out.println("B = " + b.getBalance()); // 1500000
    }

    // ------------------------------------------------

    static void test10_totalBalanceInvariant() throws Exception {

        System.out.println("\nTEST 10 : Total Balance Invariant");

        Account a = new Account(1, 5000);
        Account b = new Account(2, 5000);

        int initialTotal = a.getBalance() + b.getBalance();

        Thread[] threads = new Thread[20];

        for (int i = 0; i < 20; i++) {
            threads[i] = new Thread(() -> BankService.transfer(a, b, 50));
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        int finalTotal = a.getBalance() + b.getBalance();

        System.out.println("Initial Total = " + initialTotal);
        System.out.println("Final Total = " + finalTotal);
    }
}