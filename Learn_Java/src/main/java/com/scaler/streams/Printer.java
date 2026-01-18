package com.scaler.streams;

@FunctionalInterface
public interface Printer<T> {
	void print(T type);
}
