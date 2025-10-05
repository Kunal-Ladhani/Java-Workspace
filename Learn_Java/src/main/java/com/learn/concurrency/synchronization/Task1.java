package com.learn.concurrency.synchronization;

public class Task1 implements Runnable {

	@Override
	public void run() {

		// Separate objects per Thread
		// -> both threads will have separate objs
		// -> will look like they are entering critical section (sync block) simultaneously!
		// -> but each thread will have separate monitor lock on their resp objects.
		Object obj = new Object();

		synchronized (obj) {
			System.out.println(Thread.currentThread().getName() + " inside 1st sync block!");
			try {
				System.out.println(Thread.currentThread().getName() + " is sleeping!");
				Thread.sleep(5000l);
			} catch (InterruptedException e) {
				System.out.println("Thread interrupted!");
			}
		}

		synchronized (obj) {
			System.out.println(Thread.currentThread().getName() + " inside 2nd sync block!");
		}

		synchronized (obj) {
			System.out.println(Thread.currentThread().getName() + " inside 3rd sync block!");
		}
	}

}


