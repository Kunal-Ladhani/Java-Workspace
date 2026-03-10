package com.scaler.concurrency.deprecated_methods;

public class Driver {

    public static void main(String[] args) {
        System.out.println(Thread.currentThread().getName() + " has STARTED");

        SharedResource resource = new SharedResource();

        Thread t1 = new Thread(() -> {
            System.out.println(Thread.currentThread().getName() + " is calling shared resource.");
            resource.doSomething();

        }, "oggy-thread");


        Thread t2 = new Thread(() -> {
            try {
                Thread.sleep(1000L);
            } catch (Exception e) {
                // do log the exp.
            }

            System.out.println(Thread.currentThread().getName() + " is calling shared resource.");
            resource.doSomething();

        }, "jack-thread");

        t1.start();
        t2.start();

        try {
            t1.suspend();
        } catch (Exception e) {
            // log the exp
        }

        // THIS IS NEEDED OTW T2 (jack) will keep waiting for T1(oggy) to finish its work (but it never does release the lock)
        // means jack can never enter oggy's house of sorts XD
        try {
            Thread.sleep(2000L);
            t1.resume();
        } catch (Exception e) {
            // log the exp
        }

        System.out.println(Thread.currentThread().getName() + " has ENDED");
    }

}
