package com.learn.serialization;

import java.io.*;

public class HasARelationshipInSerialization {
	// class has a student
	// if your outer class is serialized
	// automatically all the inner classes will get serialized
	// this is like an object graph
	public static void main(String[] args) throws IOException {
		
		Dog dog = new Dog();
		
		Parent p = new Parent();
		
		FileOutputStream fileOutputStream = new FileOutputStream("nio.txt");
		
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
		
		objectOutputStream.writeObject(p);
		
		objectOutputStream.close();
		
		System.out.println("Object is serialized.");
	}
}



class Dog implements Serializable {
	Cat cat = new Cat();
}


// if any inner class is not serializable -> NotSerializableException
class Cat implements Serializable {
	Mouse mouse = new Mouse();
}

class Mouse implements Serializable {
	public void noise() {
		System.out.println("Chu Chu...");
	}
}

// if parent does not implement serializable -> child class still can
class Parent implements Serializable {
	int age = 40;
}

class Child extends Parent implements Serializable {
	int age = 15;
	int height = 130;
}