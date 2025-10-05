package com.learn.concurrency.questions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Sorter implements Callable<List<Integer>> {

	private List<Integer> arrayToSort;

	public Sorter(List<Integer> arrayToSort) {
		this.arrayToSort = arrayToSort;
	}

	@Override
	public List<Integer> call() throws Exception {

		if (arrayToSort.size() <= 1) {
			return arrayToSort;
		}

		int size = arrayToSort.size();

		int mid = size / 2;

		List<Integer> leftArray = new ArrayList<>(mid);
		List<Integer> rightArray = new ArrayList<>(mid);

		for (int i = 0; i < mid; ++i) {
			leftArray.add(arrayToSort.get(i));
		}

		for (int i = mid; i < size; ++i) {
			rightArray.add(arrayToSort.get(i));
		}

		ExecutorService executor = Executors.newCachedThreadPool();

		Sorter leftArraySortingTask = new Sorter(leftArray);
		Sorter rightArraySortingTask = new Sorter(rightArray);

		Future<List<Integer>> sortedLeftArray = executor.submit(leftArraySortingTask);
		Future<List<Integer>> sortedRightArray = executor.submit(rightArraySortingTask);

		List<Integer> sortedLeft = sortedLeftArray.get();
		List<Integer> sortedRight = sortedRightArray.get();

		int i = 0, j = 0;
		List<Integer> sortedMergedArray = new ArrayList<>();

		while (i < mid && j < mid) {
			if (sortedLeft.get(i) <= sortedRight.get(j)) {
				sortedMergedArray.add(sortedLeft.get(i++));
			} else {
				sortedMergedArray.add(sortedRight.get(j++));
			}
		}

		while (i < sortedLeft.size()) {
			sortedMergedArray.add(sortedLeft.get(i++));
		}

		while (j < sortedRight.size()) {
			sortedMergedArray.add(sortedRight.get(j++));
		}


		return  sortedMergedArray;
	}
}
