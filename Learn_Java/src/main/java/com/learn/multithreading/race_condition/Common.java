package com.learn.multithreading.race_condition;

public class Common {
	// area shared by multiple threads -> critical section

	// add synchronized keyword before return type of critical section

	// or -> public synchronized void fun(String name) {----}	
	public static synchronized void fun(String name) {
		/*
			read only section (non-critical section)
			.
			.
			.
		*/

		System.out.println("Welcome,");
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
		}
		System.out.println(name + ".");

		/*
			read only section (non-critical section)
			.
			.
			.
		*/
	}

	public synchronized void fun1(String name) {
		System.out.println("Hello, ");
		try {
			Thread.sleep(2000);
			System.out.println(name);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println(" from common area");
	}

}



