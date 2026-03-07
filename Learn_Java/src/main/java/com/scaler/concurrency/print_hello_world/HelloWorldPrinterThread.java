package com.scaler.concurrency.print_hello_world;

import java.util.stream.IntStream;

public class HelloWorldPrinterThread implements Runnable {

	private void doSomething() {
		System.out.println("Starting something! from: " + Thread.currentThread().getName());

		// blocking the thread
		// thread is busy waiting

//		for (int i = 1; i <= 1_00_000; i++) {
//			System.out.println("waiting... time = " + i);
//		}

		IntStream.rangeClosed(1, 1_00_000).forEach(i -> System.out.println("waiting... time = " + i));

		System.out.println("Ending something! from: " + Thread.currentThread().getName());
	}

	@Override
	public void run() {
		System.out.println("Hello world! from: " + Thread.currentThread().getName());
		doSomething();
	}
}
