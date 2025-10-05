package com.learn.multithreading.revision;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MultithreadingRevision {

	public static void main(String[] args) throws ExecutionException, InterruptedException {

		/*
		 * Creating Threads -
		 * 1. extend thread class
		 * 2. implement Runnable -> functional programming has made i t easier -> functional interfaces, lambda expressions, anonymous class etc.  etc.
		 * 3. implement Callable
		 * 4. Executor Service for creating, running and managing threads.
		 */

		// Extending the thread class
		Thread task1 = new Task1();
		task1.start();

		System.out.println("---------------------------------");

		// Creating instance of runnable
		Task2 task2 = new Task2();
		// Passing instance of runnable class to constructor
		Thread thread2 = new Thread(task2);
		thread2.start();

		System.out.println("---------------------------------");

		Task3 task3 = new Task3();
//		Thread thread3 = new Thread(task3);  // -> cannot do like this, you have to use executor service

		ExecutorService executorService = Executors.newFixedThreadPool(5);
		Future<Integer> future = executorService.submit(task3);


		System.out.println("My val = " + future.get());

		executorService.shutdown();
	}

}
