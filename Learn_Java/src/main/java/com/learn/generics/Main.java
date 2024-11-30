package com.learn.generics;

import com.learn.generics.badCode.MyList;
import com.learn.generics.goodCode.MyGenericList;

public class Main {

	public static void main(String[] args) {
		MyList list = new MyList();
		list.add(1);
		list.add("lancelot");
		Student s = new Student(10, "mathew");
		list.add(s);

		int i = (int) list.get(0);
		String name = (String)list.get(1);
		Student kamal = (Student) list.get(2);
		// how will the caller know what type it is returning
		// caller cannot determine it at compile time
		// plus caller will have to cast it into known class type
		// what if you accidentally cast it into wrong type ?
		// it will throw class cast exception

		MyGenericList<Student> students = new MyGenericList<>();
		students.add(s);
		// students.add("kunal");	will throw error at compile time
		// compile time type safety

	}

}
