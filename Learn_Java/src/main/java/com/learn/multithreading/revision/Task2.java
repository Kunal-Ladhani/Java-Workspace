package com.learn.multithreading.revision;

public class Task2 implements Runnable {

	static {
		System.out.println("task2 -> static block");
	}

	{
		System.out.println("task2 -> non-static block");
	}

	public Task2() {
		System.out.println("task2 -> constructor");
	}

	@Override
	public void run() {
		System.out.println("task2 run method -> task2 started");
		for (int i = 11; i <= 20; i++) {
			System.out.print(i + " ");
		}
		System.out.println("\ntask2 run method -> task2 completed");
	}
}
