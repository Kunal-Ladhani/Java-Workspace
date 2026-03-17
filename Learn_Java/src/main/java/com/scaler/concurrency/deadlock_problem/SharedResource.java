package com.scaler.concurrency.deadlock_problem;

public class SharedResource {

    public void doSomething(Object obj1, Object obj2) {

        // outer block
        synchronized (obj1) {
            System.out.println(Thread.currentThread().getName() + " ENTERED outer block...");
            try {
                System.out.println(Thread.currentThread().getName() + " is SLEEPING for 10s...");
                Thread.sleep(10_000L);

                System.out.println(Thread.currentThread().getName() + " WAITING outside inner block...");
                // inner block
                synchronized (obj2) {
                    System.out.println(Thread.currentThread().getName() + " ENTERED inner block...");
                    try {
                        System.out.println(Thread.currentThread().getName() + " is SLEEPING for 8s...");
                        Thread.sleep(8_000L);
                    } catch (Exception e) {
                        // log the exp
                    }
                    System.out.println(Thread.currentThread().getName() + " EXITING inner block...");
                }

                System.out.println(Thread.currentThread().getName() + " EXITING outer block...");
            } catch (Exception e) {
                // log the exp
            }
        }

    }

}
