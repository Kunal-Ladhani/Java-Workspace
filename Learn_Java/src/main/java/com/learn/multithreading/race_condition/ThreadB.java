package com.learn.multithreading.race_condition;

public class ThreadB extends Thread {

	private final String name;

	public ThreadB(String name) {
		this.name = name;
	}

	@Override
	public void run() {
		Common.fun(name);
	}
}


