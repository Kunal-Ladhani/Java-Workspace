package com.scaler.concurrency.shared_resource;

public class Main {

	public static void main(String[] args) {

		System.out.println("main method start");

		SharedResource sharedResource = new SharedResource();

		Thread producerThread = new Thread(new ProducerTask(sharedResource));
		producerThread.setName("Producer-Thread");

		// consumer thread created => can also be done using lambda expression
		Thread consumerThread = new Thread(new ConsumerTask(sharedResource));
		consumerThread.setName("Consumer-Thread");

		// thread is in RUNNABLE state
		producerThread.start();
		consumerThread.start();

		System.out.println("main method end");

	}

}
