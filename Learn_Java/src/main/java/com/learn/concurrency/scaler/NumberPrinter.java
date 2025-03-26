package com.learn.concurrency.scaler;

public class NumberPrinter implements Runnable {

	private final int NUMBER_TO_PRINT;

	public NumberPrinter(int x) {
		this.NUMBER_TO_PRINT = x;
		if (x <= 10) {
			Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
		}
	}

	@Override
	public void run() {
		System.out.println("num: " + this.NUMBER_TO_PRINT + "\t Thread: " + Thread.currentThread().getName());
		if (this.NUMBER_TO_PRINT == 100) {
			Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
			System.out.println("Thread: " + Thread.currentThread().getName() + " has priority: " + Thread.currentThread().getPriority());
		}
	}

}
