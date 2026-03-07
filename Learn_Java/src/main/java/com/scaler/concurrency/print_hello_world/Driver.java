package com.scaler.concurrency.print_hello_world;

public class Driver {


	public static void main(String[] args) {

		HelloWorldPrinterThread helloWorldPrinterThread = new HelloWorldPrinterThread();
		helloWorldPrinterThread.run();

		Thread t = new Thread(helloWorldPrinterThread);
		t.start();

	}


}
