package com.learn.concurrency.scaler;

class PrintNumbers {
	private final int MAX = 10;
	 private int number = 1;
	private boolean isOddTurn = true; // Flag to alternate between threads

	public synchronized void printOdd() {
		while (number <= MAX) {
			while (!isOddTurn) { // If it's not the odd thread's turn, wait
				try {
					wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			if (number > MAX) break; // Exit condition
			System.out.println(Thread.currentThread().getName() + " - " + number++);
			isOddTurn = false; // Switch turn
			notifyAll(); // Notify the other thread
		}
	}

	public synchronized void printEven() {
		while (number <= MAX) {
			while (isOddTurn) { // If it's not the even thread's turn, wait
				try {
					wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			if (number > MAX) break; // Exit condition
			System.out.println(Thread.currentThread().getName() + " - " + number++);
			isOddTurn = true; // Switch turn
			notifyAll(); // Notify the other thread
		}
	}
}