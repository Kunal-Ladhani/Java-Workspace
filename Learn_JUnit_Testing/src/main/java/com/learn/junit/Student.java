package com.learn.junit;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class Student implements Externalizable {

	private int studentId;
	private String name;
	private String email;
	private String password;

	// if you want to save the state of only 2 fields.
	// you will have to mark all else -> n-2 fields as transient.
	// else use Externalizable interface.
	// and put in these methods that what all fields you want to serialize.
	
	/*
	Externalization serves the purpose of custom Serialization, where we can decide what to store in stream.
	Externalizable interface present in java.io, is used for Externalization which extends Serializable interface. It consist of two methods which we have to override to write/read object into/from stream which are- 
	
	Key differences between Serializable and Externalizable 
 

    Implementation : Unlike Serializable interface which will serialize the variables in object with just by implementing interface, here we have to explicitly mention what fields or variables you want to serialize.
    
    Methods : Serializable is marker interface without any methods. Externalizable interface contains two methods: writeExternal() and readExternal().
    
    Process: Default Serialization process will take place for classes implementing Serializable interface. Programmer defined Serialization process for classes implementing Externalizable interface.
    
    Backward Compatibility and Control: If you have to support multiple versions, you can have full control with Externalizable interface. You can support different versions of your object. If you implement Externalizable, it’s your responsibility to serialize super class.
    
    public No-arg constructor: Serializable uses reflection to construct object and does not require no arg constructor. But Externalizable requires public no-arg constructor.
	
	*/


	// to write objects to stream
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(studentId);
		out.writeObject(name);
		out.writeObject(email);
	}

	// to read objects from stream
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		int studId = in.readInt();
		String studName = (String) in.readObject();
		String studEmail = (String) in.readObject();
	}
	
	/*
	private void writeObject(ObjectOutputStream out) throws IOException {
		throw new NotSerializableException();
	}
	
	private void readObject(ObjectInputStream in) throws IOException {
		throw new NotSerializableException();
	}
	*/
}
