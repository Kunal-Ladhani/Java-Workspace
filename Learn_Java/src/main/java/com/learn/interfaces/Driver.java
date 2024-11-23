package com.learn.interfaces;

public class Driver {

	public static void main(String[] args) {
		Driver d = new Driver();

		X x = d.funDriver();    // hold in interface reference
		// X x = new Ximpl(); -> this is same

		if (x != null) {
			// you can access all the members inside X interface
			// or those that are of X and are implemented in Ximpl class

			// basically all left side methods can be used!

			x.fun2();
			x.fun3();
		} else {
			System.out.println("NULL PTR EXCEPTION");
		}

		Object obj = d.funDriver();    // hold in Object reference
		// Object obj = new Ximpl() -> same as this


		// can access only method of Object class or that are inherited

		X x1 = (X) obj; // downcast to interface - 1st level
		x1.fun2();
		x1.fun3();    // for default method access you need an implementation class object
		X.fun1(); // can access only the static methods
		System.out.println(X.I);

		Ximpl x2 = (Ximpl) x1; // 2nd level of downcast - to implementation class object
		x2.fun2();
		x2.fun3();
		x2.fun4();

		Ximpl x3 = (Ximpl) obj; // direct downcast to implementation object (1st level)
		x3.fun2();
		x3.fun3();
		x3.fun4();

		// 	you have to hold the value of a function in a reference of type same as the return type of func
		//	type mentioned in function signature or bigger type.
		//	does not matter if the function is actually returning a smaller type or anything.
		// this is the responsibility of function caller.

		// author should return object of same type as return type.
		// or smaller type -> child class object.
		// this is function author's responsibility.

		// same as interface or Object variable.

		// if any method is returning an object then always before doing anything, check for null.
		// or it can give NullPointerException.

	}

	public X funDriver() {
		System.out.println("inside funDriver of Driver");
		// return null;
		return new Ximpl(); // implementation class object
	}
}
