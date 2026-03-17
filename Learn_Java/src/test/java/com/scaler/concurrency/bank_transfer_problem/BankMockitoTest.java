package com.scaler.concurrency.bank_transfer_problem;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

public class BankMockitoTest {

    @Test
    void verifyWithdrawAndDepositCalled() {

        Account from = Mockito.spy(new Account(1, 1000));
        Account to = Mockito.spy(new Account(2, 1000));

        BankService.transfer(from, to, 100);

        verify(from, times(1)).withdraw(100);
        verify(to, times(1)).deposit(100);
    }

}