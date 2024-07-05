package com.learn.multithreading;

// join

// sum thread -> calculate the sum
class Sum implements Runnable {
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

public class ConditionalSuspention {
	// main thread -> print the sum
	public static void main(String[] args) throws InterruptedException {
		System.out.println(Thread.currentThread().getName() + " thread starts...");
		Sum sumObj = new Sum();
		Thread sumThread = new Thread(sumObj);
		sumThread.start(); // Calculates the sum


		// main thread should join only after sumThread is done executing
		// sumThread -> mainThread

		sumThread.join();
		// jiss thread me call kia hai wo thread wait karegi dusri wali ke end hone ka

		System.out.println(sumObj.sum);
		// 0 because main thread runs first 

		System.out.println(Thread.currentThread().getName() + " thread ends...");
	}
}
