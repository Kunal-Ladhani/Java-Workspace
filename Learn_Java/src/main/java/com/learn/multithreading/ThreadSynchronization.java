package com.learn.multithreading;

class personalThread extends Thread {
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

public class ThreadSynchronization {

	public static void main(String[] args) throws InterruptedException {
		personalThread thread = new personalThread();

		Thread.currentThread().setName("Main Thread");

		thread.start();
		//Thread.sleep(5000);
//		try {
//			Thread.sleep(3000);			
//		} catch(InterruptedException e) {
//			e.printStackTrace();
//		}
		// we can either put it to sleep but it is very vague,
		// because we don't know how long the thread will take to complete its task.
		// better to use wait method

		synchronized (thread) {
			System.out.println(Thread.currentThread().getName() + " is going into wait state.");
			// this line means that
			// main will wait for personal thread
			thread.wait();
			System.out.println(Thread.currentThread().getName() + " got notified.");

		}

		System.out.println("Total : " + thread.total);
	}
}
