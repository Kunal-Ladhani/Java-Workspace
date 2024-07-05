package com.learn.interfaces;

public interface X {

	public int I = 10;
	// automatically static and final

	public static void fun1() {
		System.out.println("Inside static function of interface");
	}

	public default void fun2() {
		System.out.println("Inside default function of interface");
	}

	public void fun3();    // can't have implementation of a function
}
