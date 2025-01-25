package com.learn.concurrency;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Driver {

	public static void main(String[] args) {
		
//		Thread thread = new HelloWorldPrinter();
//		thread.start();
		
		System.out.println("-------------------BASIC RUNNER-----------------------");
		
//		for(int i=1; i<=100; i++) {
//			Thread thread = new Thread(new NumberPrinter(i));
//			thread.start();
//		}	

		// we had multiple threads here >10 means
		
		
		System.out.println("-------------------PROD RUNNER-----------------------");
				
		// we have only 10 threads here.
		// we cannot predict the order of thread execution here.
		// it can have multiple thread pool.
		// can set priority of thread pools.
		
		
		Executor executor = Executors.newFixedThreadPool(10);
		// multiple threads -> 10 threads
		
		Executor sizeOneExecutor = Executors.newFixedThreadPool(1); 
		Executor singleThreadedExecutor = Executors.newSingleThreadExecutor();
		
		//	single threaded operations
		// only one thread is there -> main thread.
		
//		for(int i=1; i<=100; i++) {
//			sizeOneExecutor.execute(new NumberPrinter(i));
//		}

		System.out.println("------------------MERGE SORT------------------------");
		
		
		
	}
}
