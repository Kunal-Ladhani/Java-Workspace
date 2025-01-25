package com.learn.multithreading;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class CallableInterface {

	public static void main(String[] args) throws InterruptedException, ExecutionException {
		MyCallable[] jobs = {
				new MyCallable(10),
				new MyCallable(20),
				new MyCallable(30),
				new MyCallable(40),
				new MyCallable(50)
		};

		ExecutorService executorService = Executors.newFixedThreadPool(2);

		for (MyCallable myCallable : jobs) {
			Future future = executorService.submit(myCallable);
			// you may or may not want to store the submit() method return value in Future object
			// future is a calculation in progress
			// then you can print it
			System.out.println(future.get());
			// will throw an exception that you need to handle
		}
		executorService.shutdown();
	}

}

