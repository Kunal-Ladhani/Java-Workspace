package com.scaler.generics;

public class GenericMethods<S> {

	public void doSomething(S name) {
	}

	public static <K> void printName(K name) {
		// <K> tells to expect K type in parameter .
		// it will tell nothing about the datatype of name parameter.
		System.out.println(name);
	}


	public static <T> T getType(T type) {
		return type;
		// static works on its own doesnt depend on the type of class
		// it just intimates the getType static method
		// you should have a return type that should not depend on the time of object creation
		// you cannot return anything apart from T type of data.
	}
}
