package com.learn.concurrency.scaler;

public class Driver {
	public static void main(String[] args) {
		HelloWorldPrinter printer = new HelloWorldPrinter();
//		printer.run();
		Thread t = new Thread(printer);
		t.start();

		System.out.println("Hello! from: " + Thread.currentThread().getName());

	}
}
