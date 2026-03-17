package com.scaler.concurrency.synchronization;

public class SharedResource {

    public synchronized void fun1() {
        try {
            Thread.sleep(5000L);
        } catch (Exception e) {
            // log the exp here
        }
    }

    public void fun2() {

        System.out.println(Thread.currentThread().getName() + " start fun2");

        synchronized (this) {
            try {
                System.out.println(Thread.currentThread().getName() + " sleeping in 1st sync block");
                Thread.sleep(15_000L);
            } catch (Exception e) {
                // log the exp here
            }
        }

        System.out.println(Thread.currentThread().getName() + " is in middle of fun2");

        // since lock is on shared object between threads then it will not allow to go inside
        synchronized (this) {
            try {
                System.out.println(Thread.currentThread().getName() + " sleeping in 2nd sync block");
                Thread.sleep(10_000L);
            } catch (Exception e) {
                // log the exp here
            }
        }

        System.out.println(Thread.currentThread().getName() + " end fun2");
    }

}
