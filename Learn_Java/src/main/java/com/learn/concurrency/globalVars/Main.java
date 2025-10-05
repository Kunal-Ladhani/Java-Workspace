package com.learn.concurrency.globalVars;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Main {

	public static void main(String[] args) throws ExecutionException, InterruptedException {
		int startingNumber = 0;

		Value value = new Value(startingNumber);

		Adder adder = new Adder(value);
		Substractor substractor = new Substractor(value);

		ExecutorService executor = Executors.newFixedThreadPool(5);

		Future<Void> adderFuture = executor.submit(adder);
		Future<Void> substractorFuture = executor.submit(substractor);

		adderFuture.get();
		substractorFuture.get();

		System.out.println("Final value = " + value.number);

		executor.shutdown();
	}

}
