package com.learn.concurrency.synchronization;

public class Task4 implements Runnable {

	@Override
	public void run() {

		// synchronized block acquiring lock on this object
		synchronized (this) {
			System.out.println(Thread.currentThread().getName() + " inside 1st sync method!");
			try {
				System.out.println(Thread.currentThread().getName() + " is sleeping!");
				Thread.sleep(5000l);
			} catch (InterruptedException e) {
				System.out.println("Thread interrupted!");
			}
		}
	}

}
