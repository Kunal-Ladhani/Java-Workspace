package com.scaler.streams;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class Driver {

	public static void main(String[] args) {

		// Stream - way of making object of class that impl just one function
		// option 1
		// impl that interface
		// create obj of that impl class
		// use that one function.
		MyFunctionalInterface implClasObj = new MyImplClass();
		implClasObj.myMethod(44);

		// option 2
		// no need to write a class
		// create a lambda expression for that
		// its basically a anonymous function
		// fn is reference variable of Fn interface which points to anonymous class object impl only one method
		MyFunctionalInterface fn = (Integer num) -> {
			System.out.println("Print num+1 from = " + num + 1);
		};
		fn.myMethod(55);

		// lambda fn can take variables from same scope in which it is present

		MathOp<Integer> adder = (a, b) -> {
			return a + b;
		};

		// if it is just 1 statement you can lose the brackets and the return statement
		MathOp<Integer> subtractor = (a, b) -> a - b;
		MathOp<Integer> multiplier = (a, b) -> a * b;
		MathOp<Integer> divider = (a, b) -> a / b;

		// If it is taking only 1 arg then you can lose the bracket too!
		Printer<String> p1 = (String s) -> {
			System.out.println("p1 printed - " + s);
		};

		Printer<String> p2 = (String s) -> System.out.println("p2 printed - " + s);

		Printer<String> p3 = s -> System.out.println("p3 printed - " + s);

		// You can lose everything and use pre-written implementation and pass a method reference
		Printer<String> p4 = System.out::println;

		p1.print("Kunal");
		p2.print("Kunal");
		p3.print("Kunal");
		p4.print("Kunal");

		List<Integer> list = Arrays.asList(1, 2, 4, -1, 6, 2, 0, -4, 5, 11, -15, 6);
		Stream<Integer> listStream = list.stream();

		long negatives = listStream.filter(e -> e < 0).count();
		System.out.println("negatives in list = " + negatives);

		// stream once closed cannot be operated upon later
		// it is closed !
//		List<Integer> negs = listStream.filter(e -> e < 0).toList();

		List<Integer> negs = list.stream()
				.filter(e -> e>=0)
				.sorted((a,b)->b-a)
				.toList();
		System.out.println(negs);



	}

}
