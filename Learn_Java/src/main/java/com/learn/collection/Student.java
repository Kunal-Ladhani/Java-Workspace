package com.learn.collection;


import java.util.Objects;

public class Student implements Comparable {
	
	private int roll;
	private String name;
	
	public Student(int roll, String name) {
		this.name = name;
		this.roll = roll;
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
	
	
	@Override
	public String toString() {
		return "Student [roll=" + roll + ", name=" + name + "]";
	}

	// this is super important concept

	@Override
	public int hashCode() {
		return Objects.hash(name, roll);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Student other = (Student) obj;
		return Objects.equals(name, other.name) && roll == other.roll;	// we did this
	}
/*
	@Override
	public boolean equals(Object obj) {
		Student s1 = this;
		Student s2 = (Student)obj;
		
		return (s1.getRoll() == s2.getRoll()) && (s1.getName().equals(s2.getName()));
	}
	
	@Override
	public int hashCode() {
		return roll;
	}
*/
	
	
	// define the sorting technique for multiple Student objects
	@Override
	public int compareTo(Object obj) {
		// TODO Auto-generated method stub
		Student s1 = this;
		Student s2 = (Student)obj;
		
//		if(s1.roll > s2.roll)
//			return 1;
//		else if(s1.roll < s2.roll)
//			return -1;
//		else
//			return 0;
		
		return s1.roll > s2.roll ? 1 : -1;
		// ternary operator
	}

	
} 
