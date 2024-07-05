package com.learn.collection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.TreeSet;

public class LearnSet {
	public static void main(String[] args) {
		// HashSet internally uses HashMap
		// load factor or feel ratio
		
		HashSet hs1 = new HashSet();
		HashSet hs2 = new HashSet(1000,0.8f);
		
		hs1.add(null);
		hs1.add(10);
		hs1.add("Kunal");
		
		System.out.println(hs1);
		System.out.println(hs1.size());
		
		System.out.println("====================================");
		
		HashSet<Integer> set = new HashSet<>();
		
		
		// if you want that input sequence should be preserved then use LinkedHashSet
		
		LinkedHashSet<Integer> ls = new LinkedHashSet<>();
	
		
		// remove duplicate elements from ArrayList
		ArrayList<Integer> al = new ArrayList<>();
		al.add(10);
		al.add(10);
		al.add(20);
		al.add(20);
		al.add(30);
		al.add(30);
		al.add(40);
		
		System.out.println(al);
	
		// 2 approach are there
		// 1st approach - 
		
		HashSet<Integer> hs = new HashSet<>();
		// only stores unique values
		
		LinkedHashSet<Integer> lhs = new LinkedHashSet<>();
		// order is preserved
		
		
		for(Integer i : al) {
			hs.add(i);
			lhs.add(i);
		}
		
		System.out.println(hs);
		System.out.println(lhs);
		
		// 2nd approach -
		
		LinkedHashSet<Integer> lhs2 = new LinkedHashSet<>(al);
		// just pass the ArrayList to constructor
		
		ArrayList<Integer> al2 = new ArrayList<>(lhs2);
		
		System.out.println(al2);
		
		// you can convert any collection class to other coll class by simply passing object to constructor of coll class
		
		// if you use String then those many objects will be created after every iteration
		// use StringBulder instead along with append method
		// only one object will be created
		// .toString() will convert any object to String
		
		Student s1 = new Student(10,"Ram");
		Student s2 = new Student(10,"Ram");
		System.out.println(s1 == s2);	//false
		System.out.println(s1.equals(s2)); // false
		// equals() is inherited from object class (it also does == in Object class)
		// so override equals() of object
		
		System.out.println("================================================");
		
		HashSet<Student> hset = new HashSet<>();
		
		hset.add(s1);
		hset.add(s2);
		
		
		System.out.println(hset.size()); 	// as HashSet considers both students to be different
		// hashCode() is also present in Object class
		// it is bestfriend of equals() method
		
		// whenever you are overriding equals() then you also override hashCode() 
		// so that for logically equal object it returns same hashCode
		
		
		System.out.println(s1.hashCode());
		System.out.println(s2.hashCode());
		
		// hashCode is int
		// even though both student are logically equal
		// hashCode are different
		// so they re stored separately in HashSet
		
		// roll is unique for each student 
		// it is like primary key - col using which you can identify row uniquely
		
		// hashcode should return same int val each time for that object
		// add() interanlly calls hashCode and equals
		
		// contract of hashCode() and equals() in HashSet internally 
		// 1st equals() called if it returns -> true
		// 2nd it calls hashCode() if it returns -> same val
		// then only HashSet considers them logically dupicalte
		// even though they are physically unique
		
		// if you do not have a unique field
		// use Objects.hash(all fields ....)
		
		System.out.println("=============================================");
		
		TreeSet<Integer> ts = new TreeSet<>();
		
		ts.add(10);
		ts.add(10);
		ts.add(30);
		ts.add(20);
		ts.add(10);
		ts.add(30);
		ts.add(40);
		
		//ts.add(null);
		// TreeSet is only collection where null is not allowed
		
		System.out.println(ts);
		// NullPtrException
		
		// is you are using a class in treeset
		// it should implement Comparable Interface otw, ClassCastException
		// element must be comparable
		// all wrapper classes internally implement Comparable
		// you wont be able to add a single object
		
		// Comparable belongs to java.lang package
		
		TreeSet<Student> studT = new TreeSet<>();
		
		studT.add(new Student(10,"kunal"));
		studT.add(new Student(9,"kanak"));
		studT.add(new Student(46,"rohit"));
		studT.add(new Student(41,"gauri"));
		
		studT.forEach(student -> System.out.println(student));
		
		System.out.println("===========================================");
		
		// to identify duplicate elements HashSet/LinkedHashSet uses hashCode and equals() methods
		// TreeSet uses compareTo() method if it returns 0.
		
		// even if you override theses methods it does not take that route it uses different method
		// add() of TreeSet uses compareTo() method
		
		// if you want to define the sorting algorithm in the same class use Comparable interface
		// if you are not allowed to touch the class then use comparator, it also has a method
		// comparator belongs to java.util package
		// using this you can define the sorting logic outside the class
		// compare() method here is abstract
		// you can define the method outside our bean class
		// no need to pollute it, define it outside your class
		
		// just create a comparator object and pass it to TreeSet constructor	
		StudentRollComp comp = new StudentRollComp();
		TreeSet<Student> studSet = new TreeSet<>(comp);
		
		studSet.add(new Student(10,"kunal"));
		studSet.add(new Student(9,"kanak"));
		studSet.add(new Student(46,"rohit"));
		studSet.add(new Student(41,"gauri"));
		
		studSet.forEach(student -> System.out.println(student));
		
		
	}
}
