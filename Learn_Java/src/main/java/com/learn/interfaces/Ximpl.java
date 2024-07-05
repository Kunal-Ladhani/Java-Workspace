package com.learn.interfaces;

public class Ximpl implements X {
	X obj = new Ximpl(); // cannot instantiate interface

	public void fun3() {
		System.out.println("Inside function of Implementation class.");
	}

	public void fun4() {
		System.out.println("Inside function of Ximpl class");
	}
}
