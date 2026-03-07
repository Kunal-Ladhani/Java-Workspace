package com.scaler.concurrency.producer_consumer_problem;

public class Driver {

    public static void main(String[] args) {

        int bufferCapacity = 3;
        int items = 7;

//		SharedBuffer<String> buffer = new SharedBuffer<>(bufferCapacity);
        SharedQueue<String> buffer = new SharedQueue<>(bufferCapacity);

        // producer thread
        Thread producerThread = new Thread(() -> {
            for (int i = 1; i <= items; ++i) {
                buffer.produce("item-" + i);
            }
        }, "producer-thread");

        // consumer thread
        Thread consumerThread = new Thread(() -> {
            for (int i = 1; i <= items; ++i) {
                buffer.consume();
            }
        }, "consumer-thread");

        producerThread.start();

        try {
            // producer will produce first 3 (max cap) and then it will wait for consumer (we gave timeout of 5s here)
            Thread.sleep(5000L);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        consumerThread.start();
    }
}
