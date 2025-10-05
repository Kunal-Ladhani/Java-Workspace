package com.learn.concurrency.threadLifecycle;

public class MonitorLockExample {

	public synchronized void task1() {
		try {
			System.out.println("inside task1");
			Thread.sleep(1000);
		} catch (Exception e) {
			System.out.println("Exception in task1: " + e.getMessage());
		}
	}

	public void task2() {
		System.out.println("inside task2, outside synchronized");
		synchronized (this) {
			System.out.println("inside task2, inside synchronized");
		}
	}

	public void task3() {
		System.out.println("inside task3");
	}


}
