package com.learn.multithreading.create_thread;

public class myRunnable implements Runnable {

	@Override
	public void run() {
		System.out.println("hello from my Second Thread");
	}

}