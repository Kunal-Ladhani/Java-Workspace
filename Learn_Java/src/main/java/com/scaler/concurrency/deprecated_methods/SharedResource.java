package com.scaler.concurrency.deprecated_methods;

public class SharedResource {

    public boolean isAvailable = true;

    public synchronized void doSomething() {
        isAvailable = false;
        System.out.println(Thread.currentThread().getName() + " has ACQUIRED lock on shared resource");

        // wait for something to happen -> db query, network API call etc. -> some long running process
        try {
            Thread.sleep(8000L);
        } catch (Exception e) {
            System.out.println("exception - " + e.getMessage());
        }

        isAvailable = true;
        System.out.println(Thread.currentThread().getName() + " has RELEASED lock on shared resource");
    }

}
