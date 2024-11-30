package com.learn.generics.goodCode;

public class MyGenericList<T> {
	// T = type parameter - class can have parameters - type of object we need to store in list
	private T[] array = (T[]) new Object[10];
	private int size;

	public T get(int index) {
		return array[index];
	}

	public void add(T data) {
		array[size++] = data;
	}
}
