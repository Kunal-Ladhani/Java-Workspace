package com.learn.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LearnArray {
	public static void main(String[] args) {
		int[] arr = {3,5,1,2,0};
		
		System.out.println(Arrays.toString(arr));
		
		Arrays.sort(arr);	// O(NlogN)
		
		System.out.println(Arrays.toString(arr));
		
		// converts to string and returns
		
		int idx = Arrays.binarySearch(arr, 2);	// O(logN)
		
		System.out.println(idx);
		
		// binary search will work only in sorted collections, if not you need to sort first
		
		
		List<Integer> list = Arrays.asList(20,10,40,39,1);
		
		int x = Collections.frequency(list, 5);
		System.out.println(x);
		
		Collections.sort(list);
		System.out.println(list);
		
		// if you want to make list synchronized
		List<Integer> syncList = Collections.synchronizedList(list);
		System.out.println(syncList);
		
		List<Student> studentList = new ArrayList<>();
		
		studentList.add(new Student(10, "Kunal"));
		studentList.add(new Student(1, "Kanak"));
		studentList.add(new Student(20, "Kesar"));
		studentList.add(new Student(14, "Keshav"));
		
		Collections.sort(studentList,new StudentRollComp());
		// or it should implement comparable
		
		
		studentList.forEach(student -> System.out.println(student));
		Collections.reverse(list);
		
		System.out.println("==============================================");
		
		Collections.reverse(studentList);
		studentList.forEach(student -> System.out.println(student));
		list.forEach(student -> System.out.println(student));
	
	
		Student s = Collections.max(studentList);
		System.out.println(s);
		
		Object ref = null;
		System.out.println(ref instanceof Object);
		
	}
}
