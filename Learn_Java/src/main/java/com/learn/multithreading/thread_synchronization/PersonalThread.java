package com.learn.multithreading.thread_synchronization;

public class PersonalThread extends Thread {
	int total = 0;

	@Override
	public void run() {
		System.out.println("Personal Thread Starts ...");

		synchronized (this) {
			for (int i = 1; i <= 10; i++) {
				System.out.print(i + " ");
				total += i;
			}
			System.out.println();
			System.out.println("Personal thread is notifying.");
			this.notify();
		}

		System.out.println();
		System.out.println("Personal Thread Ends ...");
	}
}
