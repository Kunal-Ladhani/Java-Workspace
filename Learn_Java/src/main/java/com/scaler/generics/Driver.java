package com.scaler.generics;

public class Driver {

	public static void main(String[] args) {
		Cordinates c1 = new Cordinates();
		c1.latitude = "String";
		c1.longitude = 79.3432d;


		System.out.println("longitude = "+ (double) c1.longitude);
//		System.out.println("latitude = " + (double) c1.latitude);
		// causes -> ClassCastException
		// no compile-time safety check
		// will cause runtime error -> will fuck up in prod!!!!
		/*
			- this is too generic -> you can put anything in anyone
			- it can refer to anything means that it can store reference of anything.
			- now there is no data validation - you can put anything in anyone.
			- key can be string, array, double, map, set, boolean etc.
			- same for value.
			- it is logically wrong but syntactically ok.
			- this give too much of flexibility -> we dont want this much
			- used to happen around java 5

			- Generic types
			- Raw types
			- Generic methods
			- Inheritance and generics - bounds
			- Type erasure
		*/

		Pair<Double, Double> pair1 = new Pair<>(11.23, 42.34);
		System.out.println("first = " + pair1.getFirst());
		System.out.println("second = " + pair1.getFirst());
		// you will get compile-time type checking here!! -> that is the benefit of generics!

		Pair p2 = new Pair();
		// by default Object, Object is taken over here!!!
		// this is done for backward compatibility!!
		// this is called -> RAW TYPES
		// issues -> you are not sure about the type of variable -> doesn't give you type safety!!
		p2.setFirst(new Object());
		p2.setSecond("Kunal");
		System.out.println(p2);

		// generic types are defined at class level

		// it can also be given at method level

		// it cannot be referenced from a static context
		// means you cannot use generics in static methods or blocks
		// because it is run at time of class creation
		// and static methods belong to class not object
		// but the type will come at the time of object creation!!

		GenericMethods.printName("Kunal");

	}

}
