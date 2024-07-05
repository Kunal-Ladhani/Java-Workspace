package com.learn.basics;

import com.learn.inheritance.Address;
import com.learn.inheritance.Student;

public class Shallow_and_Deep_copy {
	
	public static void main(String[] args) {
		
		Address a = new Address();
		a.setAddress("137-B New Sindhi Colony");
		
		System.out.println("=========SHALLOW COPY==============");
		Student kunal = new Student(12,"Kunal",100,a);
		Student rohit = kunal;	//shallow copy via assignment
		
		Student kaushik = new Student(kunal.getRoll(),"Kaushik",kunal.getMarks(),kunal.getAddress());
		System.out.println(kunal.toString()); // shallow cloning
		
		a.setAddress("SINDHI COLONY");
		System.out.println(rohit.toString());
		
		a.setAddress("67 Neelkanth colony");
		System.out.println(kaushik.toString());
		
		System.out.println("==========DEEP COPY==============");
		Student kabir = (Student) kunal.clone();
		
		a.setAddress("137-B New Sindhi Colony");
		System.out.println(kunal.toString());
		System.out.println(rohit.toString());
		System.out.println(kaushik.toString());
		System.out.println(kabir.toString());
		
		Student sonu = new Student();
		sonu.setName("Sonu");
		sonu.setRoll(kunal.getRoll());
		sonu.setMarks(kunal.getMarks());
		sonu.setAddress(new Address("NEELKANTH COLONY"));

		System.out.println(sonu.toString());
		System.out.println(kunal.toString());
				
		a.setAddress("SINDHI COLONY");
		
		System.out.println(sonu.toString());
		System.out.println(kunal.toString());
		
	}

}
