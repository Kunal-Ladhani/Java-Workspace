package com.learn.multithreading;

class Common {
	// area shared by multiple threads -> critical section

	// add synchronized keyword before return type of critical section

	// or -> public synchronized void fun(String name) {----}	
	public static synchronized void fun(String name) {
		// read only section (non critical)

		System.out.println("Welcome,");
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
		}
		System.out.println(name + ".");

		// read only section (non critical)
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

class ThreadA extends Thread {
	Common c;
	String name;

	public ThreadA(Common c, String name) {
		this.c = c;
		this.name = name;
	}

	@Override
	public void run() {
		c.fun(name);
	}
}

class ThreadB extends Thread {
	Common c;
	String name;

	public ThreadB(Common c, String name) {
		this.c = c;
		this.name = name;
	}

	@Override
	public void run() {
		Common.fun(name);
	}
}


public class RaceCondition {

	public static void main(String[] args) {
		Common c = new Common();
		ThreadA t1 = new ThreadA(c, "Ram");
		ThreadB t2 = new ThreadB(c, "Shyam");

		t1.start();
		t2.start();

		//t1->t2 Welcome Welcome Ram Shyam
		//t2->t1 Welcome Welcome Shyam Ram

		// synchronized
		// will block the critical section if one is already there
		// Welcom Shyam Welcome Ram
		// Welcome Ram Welcome Shyam

		// synchronization can be done in two ways->
		// 1. Synchronized method
		// 2. synchronized block


	}

}
