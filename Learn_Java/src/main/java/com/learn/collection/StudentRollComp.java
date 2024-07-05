package com.learn.collection;

import java.util.Comparator;

public class StudentRollComp implements Comparator<Student>	 {

	@Override
	public int compare(Student s1, Student s2) {
		// TODO Auto-generated method stub
		return s1.getRoll() > s2.getRoll() ? 1 : -1;
	}
	
}
