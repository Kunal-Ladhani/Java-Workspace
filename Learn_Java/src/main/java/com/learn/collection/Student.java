package com.learn.collection;


import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;

class StudentIterator implements Iterator<Student> {

	@Override
	public boolean hasNext() {
		return false;
	}

	@Override
	public Student next() {
		return null;
	}

	@Override
	public void remove() {
		Iterator.super.remove();
	}

	@Override
	public void forEachRemaining(Consumer<? super Student> action) {
		Iterator.super.forEachRemaining(action);
	}
}

public class Student implements Comparable<Student>, Iterable<Student> {

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
		return Objects.equals(name, other.name) && roll == other.roll;    // we did this
	}

//	@Override
//	public boolean equals(Object obj) {
//		Student s1 = this;
//		Student s2 = (Student)obj;
//
//		return (s1.getRoll() == s2.getRoll()) && (s1.getName().equals(s2.getName()));
//	}
//
//	@Override
//	public int hashCode() {
//		return roll;
//	}

	// define the sorting technique for multiple Student objects
//	@Override
//	public int compareTo(Object obj) {
//		Student s1 = this;
//		Student s2 = (Student)obj;
//
//		if(s1.roll > s2.roll)
//			return 1;
//		else if(s1.roll < s2.roll)
//			return -1;
//		else
//			return 0;

//		return s1.roll > s2.roll ? 1 : -1;
	// ternary operator
//	}

	@Override
	public int compareTo(Student s2) {
		Student s1 = this;
		// ternary operator
//		return s1.roll > s2.roll ? 1 : s1.roll == s2.roll ? 0 : -1;
		return Integer.compare(s1.roll, s2.roll);
	}


	@Override
	public Iterator<Student> iterator() {

		return null;
	}

	@Override
	public void forEach(Consumer<? super Student> action) {
		Iterable.super.forEach(action);
	}

	@Override
	public Spliterator<Student> spliterator() {
		return Iterable.super.spliterator();
	}
}
