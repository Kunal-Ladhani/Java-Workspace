package com.scaler.concurrency.bank_transfer_problem;

public class BankService {

    public static synchronized void transfer(Account from, Account to, int amount) {
        // YOU IMPLEMENT THIS

        if (from.getBalance() < amount) {
            return;
        }

        from.withdraw(amount);
        to.deposit(amount);
    }

}
