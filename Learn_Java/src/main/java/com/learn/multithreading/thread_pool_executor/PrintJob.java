package com.learn.multithreading.thread_pool_executor;

public class PrintJob implements Runnable {

	private final String name;

	public PrintJob(String name) {
		this.name = name;
	}

	@Override
	public void run() {
		System.out.println(name + " job is STARTED by the thread => " + Thread.currentThread().getName());
		try {
			Thread.sleep(4000);
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
		}
		System.out.println(name + " job is COMPLETED by the thread => " + Thread.currentThread().getName());
	}

}