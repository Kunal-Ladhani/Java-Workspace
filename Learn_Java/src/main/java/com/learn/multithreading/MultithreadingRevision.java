package com.learn.multithreading;

import java.util.concurrent.*;

class Task1 extends Thread {
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
		System.out.println("task1 started");
		for (int i = 1; i <= 10; i++) {
			System.out.print(i + " ");
		}
		System.out.println("\ntask1 completed");
	}
}

class Task2 implements Runnable {

	@Override
	public void run() {
		System.out.println("task2 started");
		for (int i = 11; i <= 20; i++) {
			System.out.print(i + " ");
		}
		System.out.println("\ntask2 completed");
	}
}

class Task3 implements Callable<Integer> {

	@Override
	public Integer call() throws Exception {
		System.out.println("task3 started");
		for (int i = 21; i <= 30; i++) {
			System.out.print(i + " ");
		}
		System.out.println("\ntask3 completed");
		return 1;
	}
}


public class MultithreadingRevision {
	public static void main(String[] args) throws ExecutionException, InterruptedException {

		/*
		 * Creating Threads -
		 * 1. extend thread class
		 * 2. implement Runnable -> functional programming has made it easier -> functional interfaces, lambda expressions, anonymous class etc.  etc.
		 * 3. implement Callable
		 * 4. Executor Service for creating, running and managing threads.
		 */

		// Extending the thread class
		Thread task1 = new Task1();
		task1.start();

		// Creating instance of runnable
		Task2 task2 = new Task2();
		// Passing instance of runnable class to constructor
		Thread thread2 = new Thread(task2);
		thread2.start();

		Task3 task3 = new Task3();
		//Thread thread3 = new Thread(task3);  // -> cannot do like this, you have to use executor service

		ExecutorService executorService = Executors.newFixedThreadPool(5);
		Future<Integer> future = executorService.submit(task3);
		future.get();

		executorService.shutdown();
	}
}
