package com.learn.concurrency.questions;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

	public class NumberPrinter implements Runnable {

	private final Integer number;
	private final Map<String, Integer> map = new ConcurrentHashMap<>();

	public NumberPrinter(Integer number) {
		this.number = number;
	}

	@Override
	public void run() {
		System.out.println("number from thread - " + Thread.currentThread().getName() + " is = " + number);
		map.put(Thread.currentThread().getName(), map.getOrDefault(Thread.currentThread().getName(), 0) + 1);

		if (number == 100) {
			for(Map.Entry<String, Integer> entry : map.entrySet()) {
				System.out.println("Thread: " + entry.getKey() + " printed numbers: " + entry.getValue());
			}
		}
	}

}
