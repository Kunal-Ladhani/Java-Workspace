package com.learn.Singleton.v2;

public class Driver {
	public static void main(String[] args) {
//		Singleton firstInstance = new Singleton();
		// see, this will give error

		Singleton firstInstance = Singleton.getInstance();
		Singleton secondInstance = Singleton.getInstance();

		System.out.println(firstInstance);
		System.out.println(secondInstance);	// both will point to same location in heap memory indication both are referenceing same memory location


		char a = 'a';
		System.out.println(a + " - " + (int)a);
		char z = 'z';
		System.out.println(z + " - " + (int)z);
		char A = 'A';
		System.out.println(A + " - " + (int)A);
		char Z = 'Z';
		System.out.println(Z + " - " + (int)Z);
	}
}
