# Concurrency Interview Problem: Deadlock-Free Bank Transfer

## Problem Statement

You are designing a simple **banking system** where multiple threads can
transfer money between accounts simultaneously.

Each account is represented by the following class:

``` java
class Account {
    private int balance;

    public Account(int balance) {
        this.balance = balance;
    }

    public void deposit(int amount) {
        balance += amount;
    }

    public void withdraw(int amount) {
        balance -= amount;
    }

    public int getBalance() {
        return balance;
    }
}
```

Your task is to implement the following method:

``` java
void transfer(Account from, Account to, int amount)
```

The transfer should move money **from one account to another**.

------------------------------------------------------------------------

# Requirements

1.  The transfer must be **thread-safe**.
2.  Multiple threads should be able to transfer money between accounts
    concurrently.
3.  The **total amount of money in the system must remain constant**.
4.  The implementation must **avoid deadlocks**.

------------------------------------------------------------------------

# Example Scenario

Two threads execute the following operations simultaneously:

    Thread T1: transfer(A → B, 100)
    Thread T2: transfer(B → A, 200)

Your implementation must ensure that:

-   No inconsistent state occurs
-   No deadlocks occur
-   The balances remain correct

------------------------------------------------------------------------

# Naive Implementation (Potentially Problematic)

Many developers initially write something like:

``` java
void transfer(Account from, Account to, int amount) {
    synchronized (from) {
        synchronized (to) {
            from.withdraw(amount);
            to.deposit(amount);
        }
    }
}
```

------------------------------------------------------------------------

# Questions

## 1. Is the above implementation correct?

Explain your reasoning.

## 2. If there is a problem, describe the issue clearly.

Hint: Think about **two threads transferring money in opposite
directions**.

    T1: transfer(A, B)
    T2: transfer(B, A)

What could happen?

## 3. Design a solution that prevents the issue.

Your solution should:

-   Maintain thread safety
-   Avoid deadlocks
-   Allow maximum concurrency

------------------------------------------------------------------------

# Bonus Challenge

Assume the system contains **thousands of accounts**, and transfers
occur randomly between them.

How would you design the system to **guarantee deadlock freedom at
scale**?

Consider:

-   Lock ordering
-   Fine-grained locking
-   Explicit locks (`ReentrantLock`)
-   Other concurrency primitives

------------------------------------------------------------------------

# What the Interviewer Is Evaluating

The interviewer is looking for your understanding of:

-   Java synchronization
-   Deadlocks and their causes
-   Lock ordering strategies
-   Concurrency design principles
-   Safe shared-resource management

------------------------------------------------------------------------

# Expected Discussion Topics

Candidates often discuss:

-   Circular wait condition
-   Consistent lock ordering
-   Using `System.identityHashCode()` for lock ordering
-   Using `ReentrantLock.tryLock()`
-   Avoiding coarse-grained locking

------------------------------------------------------------------------

Good luck!