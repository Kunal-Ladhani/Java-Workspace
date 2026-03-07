package com.scaler.concurrency.merge_sort;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Driver {

	public static void main(String[] args) throws InterruptedException, ExecutionException {

		System.out.println("------------------MERGE SORT------------------------");
		
		List<Integer> values = Arrays.asList(1, 3, 5, 2, 8, 0, 4, 9);
		// can use this method also -> List.of();

		ExecutorService executor = Executors.newCachedThreadPool();
		Sorter sorter = new Sorter(values, executor);

		Future<List<Integer>> sortedValues = executor.submit(sorter);
		System.out.println(sortedValues.get());
	}

}