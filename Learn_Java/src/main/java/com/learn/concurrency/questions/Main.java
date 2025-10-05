package com.learn.concurrency.questions;

import java.util.Arrays;
import java.util.List;

public class Main {

	public static void main(String[] args) {

//		ExecutorService executor = Executors.newCachedThreadPool();

//		for (int i = 1; i <= 1_000_000; ++i) {
//			NumberPrinter printer = new NumberPrinter(i);

//			Thread thread = new Thread(printer);
//			thread.start();

//			executor.execute(printer);
//
//		}

		try {
			List<Integer> list = List.of(10, 15, 2, 9, 7, 22, 1);

			Integer[] arr1 = { 10, 15, 2, 9, 7, 22, 1 };
			List<Integer> arr2 = Arrays.asList(arr1);

			int[] arr3 = new int[] { 10, 15, 2, 9, 7, 22, 1 };
			


			 Sorter sorter = new Sorter(list);
			List<Integer> result = sorter.call();
			System.out.println("sorted list = " + result);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}


