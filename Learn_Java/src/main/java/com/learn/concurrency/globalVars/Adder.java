package com.learn.concurrency.globalVars;

import java.util.concurrent.Callable;

public class Adder implements Callable<Void> {

	private final Value value;

	public Adder(Value value) {
		this.value = value;
	}

	@Override
	public Void call() {
		for (int i = 0; i < 5000; i++) {
			synchronized (value) {
				value.number++;
			}
		}
		return null;
	}

}
