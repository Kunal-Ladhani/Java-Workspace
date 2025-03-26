package com.learn.concurrency.scaler;

public class HelloWorldPrinter implements Runnable {

	private void doSomething() {
		System.out.println("Starting something! from: " + Thread.currentThread().getName());
		for (int i = 1; i< 100000; i++) {
			System.out.print("waiting\n");
		}
		System.out.println("Ending something! from: " + Thread.currentThread().getName());
	}

	@Override
	public void run() {
		System.out.println("Hello world! from: " + Thread.currentThread().getName());
		doSomething();
	}
}
