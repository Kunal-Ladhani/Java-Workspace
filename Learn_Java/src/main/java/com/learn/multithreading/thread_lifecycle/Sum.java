package com.learn.multithreading.thread_lifecycle;

// join

// sum thread -> calculate the sum
public class Sum implements Runnable {

	int sum;

	@Override
	public void run() {
		Thread.currentThread().setName("Sum");
		System.out.println(Thread.currentThread().getName() + " Thread Starts...");
		for (int i = 0; i < 10; i++) {
			sum += (i + 1);
		}
		System.out.println(Thread.currentThread().getName() + " Thread Ends...");
	}
}
