package com.learn.multithreading;

public class ThreadLifeCycle {

	public static void main(String[] args) {

		// new state
		Thread thread1 = new Thread(
				// run method implementation
				() -> {
					System.out.println("Thread1 is running ...");
					try {
						System.out.println("Thead1 name => " + Thread.currentThread());

						Thread.currentThread().setName("Kunal's Thread");
						System.out.println("Thead1 name => " + Thread.currentThread());

						Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
						System.out.println("Thead1 priority => " + Thread.currentThread().getPriority());

						System.out.println("Thread1 sleeping...");
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					System.out.println("Thread1  waking up...");
				}
		);
		// runnable


		Thread thread2 = new Thread(
				// run method implementation
				() -> {
					System.out.println("Thread2 is running ...");
					try {

						Thread.currentThread().setName("Rohit's Thread");
						System.out.println("Thead name => " + Thread.currentThread());

						Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

						System.out.println("Thread2 sleeping...");
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					System.out.println("Thread2 waking up...");
				}
		);

		//running
		thread1.start();
		thread2.start();

		// suspend
		// USING sleep(); with try catch block

	}
}
