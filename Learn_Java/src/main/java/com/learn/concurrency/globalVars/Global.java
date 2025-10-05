package com.learn.concurrency.globalVars;

public class Global {

	private static int globalVariable = 0;

	private static class AdditionClass implements Runnable {
		@Override
		public void run() {
			for (int i = 1; i <= 5000; ++i) {
				globalVariable++;  // NOT thread-safe
			}
			System.out.println("Addition done by " + Thread.currentThread().getName());
		}
	}

	private static class SubtractionClass implements Runnable {
		@Override
		public void run() {
			for (int i = 1; i <= 5000; ++i) {
				globalVariable--;  // NOT thread-safe
			}
			System.out.println("Subtraction done by " + Thread.currentThread().getName());
		}
	}

	public static void main(String[] args) throws InterruptedException {

		// Thread creation
		Thread thread1 = new Thread(new AdditionClass(), "Adder-Thread");
		Thread thread2 = new Thread(new SubtractionClass(), "Subtractor-Thread");

		thread1.start();
		thread2.start();

		thread1.join();
		thread2.join();

		System.out.println("Final value (Race Condition) = " + globalVariable);
	}
}