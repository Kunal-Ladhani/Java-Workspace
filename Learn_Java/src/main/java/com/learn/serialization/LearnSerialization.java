package com.learn.serialization;

import java.io.*;

public class LearnSerialization {
	// transmit 3 ball from bhopal to mumbai
	// serialization -> flatten the balls at bhopal
	// send in flight
	// deserialization -> inflate at mumbai airport
	
	// we do the same with java objects -> to send over network
	
	// state of objects -> values of different fields in objects
	// we preserve the state
	// convert it into byte stream
	// write it to a file
	// this is serialization
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		
		//---------------------------SERIALIZATION-----------------------------
		
		A obj = new A();
		
		FileOutputStream fileOutputStream = new FileOutputStream("nio.txt");
		
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
		
		objectOutputStream.writeObject(obj);
		
		objectOutputStream.close();
		
		System.out.println("Object is serialized.");
		
		//---------------------------DESERIALIZATION-----------------------------
		
		FileInputStream fileInputStream = new FileInputStream("nio.txt");
		ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
		
		Object desObject = objectInputStream.readObject();
		objectInputStream.close();
		
		A deserializeObjectOfA = (A) desObject;
		
		System.out.println("Object is deserialized.");
		
		System.out.println("A.i = "+deserializeObjectOfA.i);
		System.out.println("deserializedObjOfA.email = "+deserializeObjectOfA.email);
		System.out.println("ObjOfA.email = "+obj.email);
		
	}
}

class A implements Serializable {
	int i = 10;
	
	// transient -> don't serialize this
	transient String email ="kunal.ladhani@gmail.com";
	// transient string -> null
	// transient int -> 0
	// transient boolean -> false
	
	// transient -> default value will be stored
}