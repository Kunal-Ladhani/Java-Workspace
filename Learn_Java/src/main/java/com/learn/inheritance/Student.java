package com.learn.inheritance;

public class Student implements Cloneable {
	private int roll;
	private String name;
	private int marks;
	private Address address;
	private final int schoolID;
	private final static String SCHOOL; // static blank final variable
	private final String subject;	// blank final field
	// 	A final variable that is not initialized at the time of declaration is known as blank 		final variable.
	
	static {
		SCHOOL = "ST JOSEPH";
	}	// SCHOOL is a final static so CONSTANT
	// A static blank final variable can only be initialized inside a static block
	
	{
		subject = "MATHS";
	}	// Instance initialization block
	// a final variable can be initialized at the time of declaration or inside a 		initialization block or inside all the constructors
	
	// parameters can also be final, but they should never change.
	
	// {	subject = "SCIENCE";	}
	// reassigning a final variable is not allowed
	
	// constructor can never be final as they are never inherited
	public Student(Student student) {
		this.roll = student.roll;
		this.name = student.name;
		this.marks = student.marks;
		this.address = student.address;
		this.schoolID = 1001;
	}	// copy constructor
	
	/*
	 	the copy constructor is a constructor which creates an object by initializing it with an object of the same class, which has been created previously. Java does support for copy constructors but you need to 		define them yourself.
	 */
	
	public Student() {
		this.schoolID = 1001;
		
	} // Java bean class should always have a 0 argument constructor
	
	public Student(int roll, String name, int marks) {
		super();
		this.roll = roll;
		this.name = name;
		this.marks = marks;
		this.schoolID = 1001;
	}	// this is just for our convenience not necessary.
	
	public Student(int roll, String name, int marks, Address address) {
		super();
		this.roll = roll;
		this.name = name;
		this.marks = marks;
		this.address = address;
		this.schoolID = 1001;
	}	
	
	public int getRoll() {
		return roll;
	}
	public void setRoll(int roll) {
		this.roll = roll;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getMarks() {
		return marks;
	}
	public void setMarks(int marks) {
		this.marks = marks;
	}
	
	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	@Override
	public String toString() {
		return "Student [roll=" + roll + ", name=" + name + ", marks=" + marks + ", address=" + address.getAddress() + "]";
	}
	
	@Override
	public Object clone() {
		Student student = null;
		try {
			student = (Student) super.clone();
		} catch(CloneNotSupportedException e) {
			System.out.println("Exception = "+e);
			student = new Student(this.getRoll(),this.getName(),this.getMarks(),this.getAddress());
		}
		student.address = (Address) this.address.clone();
		return student;
	}

	public int getSchoolID() {
		return schoolID;
	}
}
