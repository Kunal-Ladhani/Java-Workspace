package com.learn.serialization;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Serialization -> Writing the state of an object into a file
 * so that you can transfer this file(containing bytes) over a network or save it in a DB, or use it as a DB (use serialized version of objects as a DB)
 * so that you can later read it and deserialize it and get the same object with same state back
 * (same state means -> value of its instance variables).
 *
 * Serializable is a marker interface, otw you are not allowed to save its state, serialize it.
 */
public class SerializationRevision {
	
	public static void main(String[] args) {
        Employee emp1 = new Employee(1,"Kunal");

        // you can give any extension .tmp, .txt, .ser etc.
        // generally for serialized files we use .ser for storing this file.
        // it is convention so say that it is a special file having serialized content.

        try {
            // Serialization
            FileOutputStream fileOutputStream = new FileOutputStream("Employee.ser");
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);

            objectOutputStream.writeObject(emp1);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        } finally {
            System.out.println("Object serialized.");
        }


        try {
            // Deserialization
            FileInputStream fileInputStream = new FileInputStream("Employee.ser");
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);

            Employee deserializedEmployee = (Employee) objectInputStream.readObject();
            System.out.println(deserializedEmployee);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Object deserialized.");
        }
	}
}
