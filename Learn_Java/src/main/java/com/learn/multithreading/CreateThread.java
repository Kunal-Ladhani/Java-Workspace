package com.learn.multithreading;

public class CreateThread {
	public static void main(String[] args) {
		// 1st way
		// EXTENDING THE THREAD CLASS
		myThread firstThreadObj = new myThread();
		firstThreadObj.start();

		// 2nd way
		// CREATE A INSTANCE OF RUNNABLE INTERFACE
		// IMPLEMENT RUNNABLE INTERFACE

		// we cannot create a object of an interface

		// 3 ways - 
		// implement runnable -> see MyRunnable class below
		Thread secondThreadObject = new Thread(new myRunnable());
		secondThreadObject.start();

		// LAMBDA EXPRESSION
		Thread lambdaThreadObject = new Thread(() -> System.out.println("my Thread by Lambda way"));
		lambdaThreadObject.start();

		// ANONYMOUS INNER CLASS -> Worst Way
		Thread anonymousInnerClass = new Thread(new Runnable() {

			@Override
			public void run() {
				System.out.println("My Thread by anonymous inner class");
			}

		});
		anonymousInnerClass.start();


		System.out.println("----------------------MULTITHREADING------------------------");

		myRunnable obj = new myRunnable();
		Thread t1 = new Thread(obj);
		Thread t2 = new Thread(obj);

		t1.start();
		t2.start();

		// all will get executed simultaneously and you cannot guess the output it depends on ThreadScheduler.
		// if you have any data that will get updated by different threads it will result in race condition
		// multiple thread sharing same resource
		// one thread is trying to update the data, and other is trying to delete that data
		// race condition will be there and leads to data inconsistency

		// use synchronized keyword with methods, that leads to Thread safety in java
		//  
	}
}

class myThread extends Thread {

	@Override
	public void run() {
		System.out.println("hello from my First Thread");
		//super.run();
	}
}

class myRunnable implements Runnable {
	@Override
	public void run() {
		System.out.println("hello from my Second Thread");
	}
}