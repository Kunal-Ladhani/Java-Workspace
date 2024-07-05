package com.learn.interfaces;

class A implements Intr {

}

public class Main {

	public static void main(String[] args) {
		A a = new A();
		System.out.println(a.CONST);

		Intr i = new A();
		System.out.println(i.CONST);
	}
}
