package com.learn.multithreading.race_condition;

public class Driver {

	public static void main(String[] args) {
//		Common c = new Common();

		ThreadA t1 = new ThreadA( "Ram");
		ThreadB t2 = new ThreadB( "Shyam");

		t1.start();
		t2.start();

		//t1->t2 Welcome Welcome Ram Shyam
		//t2->t1 Welcome Welcome Shyam Ram

		// synchronized
		// will block the critical section if one is already there
		// Welcome Shyam Welcome Ram
		// Welcome Ram Welcome Shyam

		// synchronization can be done in two ways ->
		// 1. Synchronized method
		// 2. synchronized block
	}

}
