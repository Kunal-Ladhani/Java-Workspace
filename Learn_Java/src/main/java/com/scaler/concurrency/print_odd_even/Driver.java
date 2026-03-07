package com.scaler.concurrency.print_odd_even;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Driver {

	public static void main(String[] args) {
		ExecutorService executor = Executors.newCachedThreadPool();

		for (int i = 1; i <= 10_000; ++i) {
			NumberPrinter printer = new NumberPrinter(i);

			Thread thread = new Thread(printer);
			thread.start();

			executor.execute(printer);
		}
	}

}
