package com.scaler.concurrency.join_method;

public class Driver {

    public static void main(String[] args) {
        System.out.println(Thread.currentThread().getName() + " has STARTED");

        SharedResource resource = new SharedResource();

        Thread t1 = new Thread(() -> {
            System.out.println(Thread.currentThread().getName() + " is calling shared resource.");
            resource.doSomething();

        }, "OGGY");

        t1.start();

        try {
            System.out.println(Thread.currentThread().getName() + " is waiting for " + t1.getName() + " to complete execution.");
            t1.join();  // Main Thread will wait for T1 thread to join then only it will continue
        } catch (Exception e) {
            // log the exception here
        }

        // otw main will never wait (NON BLOCKING I/O) and print this line and exit (means TERMINATE)
        System.out.println(Thread.currentThread().getName() + " has ENDED");
    }

}
