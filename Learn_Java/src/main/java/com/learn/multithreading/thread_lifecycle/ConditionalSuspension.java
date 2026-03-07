package com.learn.multithreading.thread_lifecycle;


public class ConditionalSuspension {

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
