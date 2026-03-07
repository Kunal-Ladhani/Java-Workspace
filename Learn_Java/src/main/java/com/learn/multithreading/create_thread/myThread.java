package com.learn.multithreading.create_thread;

public class myThread extends Thread {

	@Override
	public void run() {
		System.out.println("hello from my First Thread");
		//super.run();
	}
}