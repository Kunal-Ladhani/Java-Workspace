package com.learn.multithreading.race_condition;

public class ThreadA extends Thread {

	private final String name;

	public ThreadA(String name) {
		this.name = name;
	}

	@Override
	public void run() {
		Common.fun(name);
	}

}