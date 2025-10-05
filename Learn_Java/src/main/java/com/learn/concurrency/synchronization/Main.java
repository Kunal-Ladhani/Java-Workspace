package com.learn.concurrency.synchronization;

public class Main {

	public static void main(String[] args) {
//		Task1 task = new Task1();
//		Task2 task = new Task2();
//		Task3 task = new Task3();
		Task4 task = new Task4();

		Thread t1 = new Thread(task, "T1");
		Thread t2 = new Thread(task, "T2");

		t1.start();
		t2.start();
	}

}