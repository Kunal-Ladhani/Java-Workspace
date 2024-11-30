package com.learn.generics.goodCode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// generic type are defined at class level
public class MyGenericClass<T, S, U, V> {

	public T myMethod(T name) {
		return name;
	}

	// cannot be referenced from a static context as object is not necessarily created.
//	public static void doSomething(T arg) {
//		return;
//	}

	// type can be given at method level
	public static <P> P displayName(P name) {
		// this will get the data type at method call
		// <P> is the data type of parameter
		// means it tells compiler that please expect type P at in the argument
		// since compiler does not know P it sets it as Object.
		return name;
	}

	public static <K extends Number> void printList(List<K> numbers) {
		numbers.forEach(System.out::println);
	}

	public static void main(String[] args) {
		String name1 = MyGenericClass.displayName("param1");
		// see it has no problem with String type as param1 is of string type.

		double value1 = MyGenericClass.displayName(123.445d);
		// it can be given double value also, no longer string is needed.

		// double value2 = MyGenericClass.displayName("karam");
		// see it will have problem in this as return type is defined to be same as given arg type

		// this works as Integer is child class of Number
		Number num = Integer.valueOf(5);

		// List<Number> numbers = new ArrayList<Integer>();
		// this will give compile time error so use extends keyword

		List<Integer> numbers = new ArrayList<>();
		Collections.addAll(numbers, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
		MyGenericClass.printList(numbers);

	}



}
