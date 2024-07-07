package com.learn.L1_Singleton.v0;

public class Driver {
	public static void main(String[] args) {
//		Singleton firstInstance = new Singleton();
		// see, this will give error

		Singleton firstInstance = Singleton.getInstance();
		Singleton secondInstance = Singleton.getInstance();

		System.out.println(firstInstance);
		System.out.println(secondInstance);	// both will point to same location in heap memory indication both are referenceing same memory location

	}
}
