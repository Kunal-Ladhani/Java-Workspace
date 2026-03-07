package com.learn.collection;

import com.learn.comparable.Student;
import com.learn.comparator.StudentRollComp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Driver {

	public static void main(String[] args) {

		List<Student> studentList = new ArrayList<>();

		studentList.add(new Student(10, "Kunal"));
		studentList.add(new Student(1, "Kanak"));
		studentList.add(new Student(20, "Kesar"));
		studentList.add(new Student(14, "Keshav"));

		Collections.sort(studentList, new StudentRollComp());

		// or it should implement comparable

		studentList.forEach(System.out::println);	// method reference -> that for each of the items in student list call this method


		System.out.println("==============================================");

		Collections.reverse(studentList);
		studentList.forEach(student -> {
			System.out.println("student -> " + student);
		});

		Student s = Collections.max(studentList);
		System.out.println(s);


	}

}
