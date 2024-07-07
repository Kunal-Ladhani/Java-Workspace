package com.learn.L3_Prototype.v0;

public class Client {

	public static void fillRegistry(StudentRegistry studentRegistry) {
		Student apr21 = new Student();
		apr21.setGradYear(2021);

		studentRegistry.register("apr21",apr21);
	}

	public static void main(String[] args) {
		StudentRegistry studentRegistry = new StudentRegistry();
		fillRegistry(studentRegistry);	// asap the main is called the registry is created and filled

		Student s1 = studentRegistry.get("apr21").clone();	// you can also use an ENUM inplace of string
		// template is cloned

		s1.setStudentId(1);
		s1.setName("Kunal");

		System.out.println(s1.toString());

	}
}
