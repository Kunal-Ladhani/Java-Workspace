package com.learn.concurrency;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class Sorter implements Callable<List<Integer>> {

	private List<Integer> values = new ArrayList<>();

	private ExecutorService executorService;

	public Sorter() {
		super();
	}

	public Sorter(List<Integer> values, ExecutorService executorService) {
		super();
		this.values = values;
		this.executorService = executorService;
	}

	@Override
	public List<Integer> call() throws Exception {
		//base case
		if(values.size() <= 1)
			return values;

		// split the array
		int mid = values.size()/2;

		List<Integer> leftList = values.subList(0, mid);
		List<Integer> rightList = values.subList(mid, values.size());

		Sorter leftSorter = new Sorter(leftList, executorService);
		Sorter rightSorter = new Sorter(rightList, executorService);

		Future<List<Integer>> sortedLeftList = executorService.submit(leftSorter);
		Future<List<Integer>> sortedRightList = executorService.submit(rightSorter);

		//merge the lists
		return merge(sortedLeftList,sortedRightList);
	}

	private List<Integer> merge(Future<List<Integer>> leftSortedFuture, Future<List<Integer>> rightSortedFuture) throws InterruptedException, ExecutionException {
		List<Integer> sortedList = new ArrayList<>();
		int first = 0;
		int second = 0;

		List<Integer> sortedLeft = leftSortedFuture.get();
		List<Integer> sortedRight = rightSortedFuture.get();

		while(first < sortedLeft.size() && second < sortedRight.size()) {
			// if left is smaller add to sorted list
			if(sortedLeft.get(first) < sortedRight.get(second)) {
				sortedList.add(sortedLeft.get(first++));
			} else {
				sortedList.add(sortedRight.get(second++));
			}
		}

		while(first < sortedLeft.size()) {
			sortedList.add(sortedLeft.get(first++));
		}

		while(second < sortedRight.size()) {
			sortedList.add(sortedRight.get(second++));
		}

		return sortedList;
	}

}
