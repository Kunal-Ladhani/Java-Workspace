package com.scaler.concurrency.print_numbers;

public class PrintNumbers {

	private final int MAX;
	private int number;
	private boolean isOddTurn; // Flag to alternate between threads

	public PrintNumbers(int MAX, int number, boolean isOddTurn) {
		this.MAX = MAX;
		this.number = number;
		this.isOddTurn = isOddTurn;
	}

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