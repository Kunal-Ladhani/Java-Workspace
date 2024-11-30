package com.learn.generics.badCode;

public class MyList {
	private int size;
	private Object[] array;

	public MyList() {
		this.array = new Object[10];
		this.size = 0;
	}

	public void add(Object data) {
		array[size++] = data;
	}

	public Object get(int index) {
		return array[index];
	}
	// types - primitive (numbers chars and booleans) and reference(anything else)
}
