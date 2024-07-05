package com.learn.junit;

public class Calculator {

	public Integer add(Integer a, Integer b) {
		return a + b;
	}

	public Integer multiply(Object a, Object b) {
		Integer v1, v2;
		if (a instanceof Integer && b instanceof Integer) {
			v1 = (Integer) a;
			v2 = (Integer) b;
		} else {
			throw new ClassCastException("Enter valid integer values.");
		}
		return v1 * v2;
	}

	public boolean methodReturnsBooleanValue() {
		// some business logic
		return true;
	}

}
