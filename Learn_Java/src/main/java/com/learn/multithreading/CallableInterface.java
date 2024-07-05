package com.learn.multithreading;

import java.util.concurrent.*;

public class CallableInterface {
	public static void main(String[] args) throws InterruptedException, ExecutionException {
		myCallable[] jobs = {
				new myCallable(10),
				new myCallable(20),
				new myCallable(30),
				new myCallable(40),
				new myCallable(50)
		};

		ExecutorService executorService = Executors.newFixedThreadPool(2);

		for (myCallable myCallable : jobs) {
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

class myCallable implements Callable {

	int num;

	public myCallable(int num) {
		this.num = num;
	}

	@Override
	public Object call() throws Exception {
		System.out.println(Thread.currentThread().getName() + " is calculating sum from 1 to " + num);
		int sum = 0;
		for (int i = 1; i <= num; i++) {
			sum += i;
		}
		return sum;
		// Object is super class of all classes so it can return a child class object -> Integer
	}

}
