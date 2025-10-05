package com.learn.concurrency.producerConsumerProblem;

import java.util.UUID;

public class Driver {

	public static void main(String[] args) {

		SharedBuffer<String> sharedBuffer = new SharedBuffer<>(3);

		// producer thread
		Thread producerThread = new Thread(() -> {
			for (int i = 0; i < 10; ++i) {
				sharedBuffer.producer(UUID.randomUUID().toString());
			}
		}, "producer-thread");

		// consumer thread
		Thread consumerThread = new Thread(() -> {
			for (int i = 0; i < 10; ++i) {
				sharedBuffer.consume();
			}

		}, "consumer-thread");

		producerThread.start();
		try {
			// producer will produce first 3 (max cap) and then it will wait for consumer (we gave timeout of 5s here)
			Thread.sleep(5000l);
		} catch (Exception e) {

		}
		consumerThread.start();
	}
}
