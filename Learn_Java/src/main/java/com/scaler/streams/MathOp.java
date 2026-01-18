package com.scaler.streams;

@FunctionalInterface
public interface MathOp<T> {

	T op(T x, T y);
}
