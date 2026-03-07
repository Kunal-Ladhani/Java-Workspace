package com.scaler.concurrency.producer_consumer_problem;

import java.util.ArrayDeque;
import java.util.Queue;

public class SharedQueue<T> {

    private final Queue<T> queue;
    private final int capacity;

    public SharedQueue(int capacity) {
        this.queue = new ArrayDeque<>();
        this.capacity = capacity;
    }

    public synchronized void produce(T item) {
        try {
            if (this.capacity == this.queue.size()) {
                System.out.println("Buffer is full! " + Thread.currentThread().getName() + " is waiting on Consumer!");
                wait();
            }
        } catch (InterruptedException e) {
            System.out.println("exception occurred: " + e.getMessage());
        }

        queue.offer(item);
        System.out.println(Thread.currentThread().getName() + " produced - " + item);

        notify();   // notify the consumer thread
    }

    public synchronized T consume() {
        try {
            if (this.queue.isEmpty()) {
                System.out.println("Buffer is empty! " + Thread.currentThread().getName() + " is waiting on Producer!");
                wait();
            }
        } catch (InterruptedException e) {
            System.out.println("exception occurred: " + e.getMessage());
        }

        T item = queue.poll();
        System.out.println(Thread.currentThread().getName() + " consumed - " + item);

        notify();   // notify the producer thread

        return item;
    }
}
