package com.scaler.concurrency.print_numbers;

public class AlternateSolution {

	public static void main(String[] args) {

		PrintNumbers printNumbers = new PrintNumbers(100, 1, true);

		Thread t1 = new Thread(() -> {
			printNumbers.printOdd();
		}, "odd-thread");

		Thread t2 = new Thread(printNumbers::printEven, "EvenThread");

		t1.start();
		t2.start();

	}
}
