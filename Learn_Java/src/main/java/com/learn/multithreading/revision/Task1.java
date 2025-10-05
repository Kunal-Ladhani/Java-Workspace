package com.learn.multithreading.revision;

public class Task1 extends Thread {
	static {
		System.out.println("task1 -> static block");
	}

	{
		System.out.println("task1 -> non-static block");
	}

	public Task1() {
		System.out.println("task1 -> constructor");
	}

	@Override
	public void run() {
		System.out.println("task1 run method -> task1 started");
		for (int i = 1; i <= 10; i++) {
			System.out.print(i + " ");
		}
		System.out.println("\ntask1 run method -> task1 completed");
	}
}
