package com.learn.concurrency;

public class HelloWorldPrinter extends Thread {

	@Override
	public void run() {
		System.out.println("Hello World");
		super.run();
	}

}
