package com.scaler.concurrency.executor;

public class HelloWorldPrinter extends Thread {

	@Override
	public void run() {
		System.out.println("Hello World");
		super.run();
	}

}
