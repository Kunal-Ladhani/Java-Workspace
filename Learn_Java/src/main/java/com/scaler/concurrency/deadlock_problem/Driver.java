package com.scaler.concurrency.deadlock_problem;

public class Driver {

    public static void main(String[] args) {

        Object obj1 = new Object();
        Object obj2 = new Object();
        SharedResource resource = new SharedResource();

        Thread thread1 = new Thread(() -> resource.doSomething(obj1, obj2), "phil-1");
        Thread thread2 = new Thread(() -> resource.doSomething(obj2, obj1), "phil-2");

        /*

        both will enter the outer block
        phil-1 will acquire monitor lock on obj1 and wait 10s
        phil-2 will acquire monitor lock on obj2 and wait 10s

        now,
         phil-1 will stand outside inner block and wait for phil-2 to release lock on obj2
        and,
         phil-2 will stand outside inner block and wait for phil-1 to release lock on obj1

        both keep waiting forever for each other ---> this is deadlock
        */

        thread1.start();
        thread2.start();

    }

}
