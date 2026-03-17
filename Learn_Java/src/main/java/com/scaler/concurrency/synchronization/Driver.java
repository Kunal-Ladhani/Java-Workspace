package com.scaler.concurrency.synchronization;

public class Driver {

    public static void main(String[] args) {
        SharedResource resource = new SharedResource();
        Thread t1 = new Thread(resource::fun2, "thread-1");
        Thread t2 = new Thread(resource::fun2, "thread-2");

        t1.start();
        t2.start();
    }

}
