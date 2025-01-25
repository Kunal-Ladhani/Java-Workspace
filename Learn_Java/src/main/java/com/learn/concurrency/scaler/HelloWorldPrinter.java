package com.learn.concurrency.scaler;

public class HelloWorldPrinter implements Runnable {

	private void doSomething() {
		System.out.println("Doing something! from: " + Thread.currentThread().getName());
	}

	@Override
	public void run() {
		System.out.println("Hello world! from: " + Thread.currentThread().getName());
		doSomething();
	}
}
