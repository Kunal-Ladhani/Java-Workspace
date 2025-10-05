package com.learn.concurrency.globalVars;

import java.util.concurrent.Callable;

public class Substractor implements Callable<Void> {

	private final Value value;

	public Substractor(Value value) {
		this.value = value;
	}

	@Override
	public Void call() {
		for (int i = 0; i < 5000; i++) {
			synchronized (value) {
				value.number--;
			}
		}
		return null;
	}

}
