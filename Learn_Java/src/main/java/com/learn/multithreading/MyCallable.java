package com.learn.multithreading;

import java.util.concurrent.Callable;

public class MyCallable implements Callable {

	int num;

	public MyCallable(int num) {
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

