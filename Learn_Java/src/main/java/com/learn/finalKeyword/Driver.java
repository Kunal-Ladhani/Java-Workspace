package com.learn.finalKeyword;

class A {

	final static String CONST;

	static {
		CONST = "variable";
	}

	final String name;
	int x;

	{
		name = "Kunal";
	}

	public A(int x) {
		this.x = x;
	}

	public void changeValueOfVar() {
		x = x + 10;
	}
}

public class Driver {
	public static void main(String[] args) {
		final A a = new A(2);
		System.out.println("Value of x = " + a.x);

		a.changeValueOfVar();

		System.out.println("Value of x = " + a.x);

		A a1 = new A(3);

		System.out.println("Value of x = " + a1.x);

		System.out.println("Value of x = " + a.x);

	}
}