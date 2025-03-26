package com.learn.concurrency.scaler;

public class Driver {

	private static void printer() {
		for (int i = 1; i <= 100; i++) {
			NumberPrinter x = new NumberPrinter(i);
			Thread t = new Thread(x);
			t.start();
		}
	}

	private static void alternatePrinter() {
		PrintNumbers printNumbers = new PrintNumbers();

		Thread t1 = new Thread(printNumbers::printOdd, "OddThread");
		Thread t2 = new Thread(printNumbers::printEven, "EvenThread");

		t1.start();
		t2.start();
	}

	public static void main(String[] args) {
//		HelloWorldPrinter printer = new HelloWorldPrinter();
//		printer.run();
//		Thread t = new Thread(printer);
//		t.start();

//		System.out.println("Hello! from: " + Thread.currentThread().getName());
//		Driver.printer();

		alternatePrinter();
	}


}
