package com.learn.concurrency.synchronization;

public class Task2 implements Runnable {

	private Object obj;

	public Task2() {
		obj = new Object();
	}

	@Override
	public void run() {

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


