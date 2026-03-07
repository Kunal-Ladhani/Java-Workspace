package com.learn.iterator;

import java.util.Iterator;
import java.util.function.Consumer;

public class EmployeeIterator implements Iterator<Employee> {

	@Override
	public boolean hasNext() {
		return false;
	}

	@Override
	public Employee next() {
		return null;
	}

	@Override
	public void remove() {
		Iterator.super.remove();
	}

	@Override
	public void forEachRemaining(Consumer<? super Employee> action) {
		Iterator.super.forEachRemaining(action);
	}
}