package com.learn.multithreading.revision;

import java.util.concurrent.Callable;

public class Task3 implements Callable<Integer> {

	static {
		System.out.println("task3 -> static block");
	}

	{
		System.out.println("task3 -> non-static block");
	}

	public Task3() {
		System.out.println("task3 -> constructor");
	}


	@Override
	public Integer call() throws Exception {
		System.out.println("task3 call method -> task3 started");
		for (int i = 21; i <= 30; i++) {
			System.out.print(i + " ");
		}
		System.out.println("\ntask3 call method -> task3 completed");
		return 1;
	}
}
