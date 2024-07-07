package com.learn.L1_Singleton.v0;

public class Singleton {
	private static Singleton instance;
	private Singleton() {
		// singleton constructor is private as we don't want external entity to create its instances
	}

	public static Singleton getInstance() {

		if(instance == null) {
			instance = new Singleton();
		}
		return instance;
	}
}
