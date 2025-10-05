package com.learn.concurrency.synchronization;

public class Task3 implements Runnable {

	// will acquire class level lock
	private static synchronized void func1() {
		System.out.println(Thread.currentThread().getName() + " inside 1st sync method!");
		try {
			System.out.println(Thread.currentThread().getName() + " is sleeping!");
			Thread.sleep(5000l);
		} catch (InterruptedException e) {
			System.out.println("Thread interrupted!");
		}
	}

	@Override
	public void run() {
		func1();
	}
}


