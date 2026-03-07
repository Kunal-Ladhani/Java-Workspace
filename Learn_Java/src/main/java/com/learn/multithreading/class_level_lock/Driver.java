package com.learn.multithreading.class_level_lock;

import com.learn.multithreading.race_condition.Common;

public class Driver {

	public static void main(String[] args) {
		Common c1 = new Common();
		Thread thread_p = new Thread(() -> c1.fun1("Ram"));

		Common c2 = new Common();
		Thread thread_q = new Thread(() -> c2.fun1("Shyam"));

		thread_p.start();
		thread_q.start();
	}
}
