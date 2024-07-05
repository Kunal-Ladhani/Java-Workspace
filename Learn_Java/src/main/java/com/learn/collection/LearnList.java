package com.learn.collection;

import java.util.*;

public class LearnList {
	public static void main(String[] args) {
		int[] arr = {1,2,3,4,5};
		
		//ArrayList<E> al = new ArrayList<>();
		ArrayList al = new ArrayList();
		// empty Arraylist object, with default capacity 10
		// this is type unsafe collection
		// it should be always type safe
		// it should be holding homogeneous elements
		
		System.out.println(al);
		System.out.println(al.size());
		
		al.add("one");
		al.add("two");
		al.add(10);
		al.add(null);
		
		System.out.println(al);
		System.out.println(al.size());

		Object o1 = al.get(0);
		System.out.println((String)o1);
		// we need to downcast after access
		
		int j = Integer.parseInt("2");
		boolean val = Boolean.parseBoolean("true");
		

		System.out.println("====================================");
		
		
		ArrayList<String> al2 = new ArrayList<>();
		// giving type on right side is not important
		// this collection is type safe
		// string or null object are allowed
		
		al2.add("Kunal");
		al2.add("Rohit");
		// you will get advantage while calling get
		System.out.println(al2.get(1));
		// we dont need to downcast, we can directly get the type object
		
		
		// iterating over arraylist arr
		for(int i=0; i<al2.size(); i++) {	// normal for loop
			System.out.println(al2.get(i));
		}
		
		for(String s : al2) {
			System.out.println(s);		// for each loop
		}
		
		al2.forEach(s -> System.out.println(s));	// lambda expression in forEach method	
		
		
		System.out.println("====================================");
		
		ArrayList<Integer> array = new ArrayList<>();
		
		array.add(10);
		array.add(20);
		array.add(30);
		array.add(40);
		array.add(50);
		array.add(60);
		array.add(70);
			
		System.out.println(array.contains(10));
		System.out.println(array.get(3));
		
		// Linked List is best DS if our frequent operation is insertion in the middle
		
		// deletion or addition element in middle of arrayList is O(N) operation
		// we solve it by using LL DS
		// LinkedList in Java follows DLL
		// each node has address of next and prev nodes.
		// addition or deletion is O(1) operation as just the pointers are changed
		
		
		LinkedList<Integer> list = new LinkedList<>();
		// here nodes will randomly be stored but you can access using index too.
		// though in LL there is no concept of index
		// LL also implements Deque Interface also
		

		System.out.println("====================================");
		
		Vector<Integer> vec = new Vector<>();
		// this is used in legacy projects
		// BSE works on 1.6v
		// most of the projects in companies use 1.8v
		// servlet JSP are deprecated
		
		
		// default it will double the capacity if new element is added
		// most of the methods are synchronized
		// arraylist methods are not synchronized
		
		System.out.println("====================================");
		
		Stack<Integer> stack = new Stack<>();
		// LIFO operation
		// most of operations are synchronized
		// as it extends vector
		stack.push(10);
		stack.push(20);
		stack.push(30);
		stack.push(40);
		stack.add(50);
		
		stack.pop();
		stack.peek();
		stack.empty();
		stack.size();
	
		System.out.println("====================================");
		
		// we can assign implementation class to the Interface ref variable

		ArrayList<Integer> Al1 = new ArrayList<>();		// too specific
		
		List<Integer> Al2 = new ArrayList<>();		// best approach to write Arraylist object
		// arraylist does not have any specific methods
		
		// dependency injection
		
		Collection<Integer> Al3 = new ArrayList<>();
		
		Object Al4 = new ArrayList<>();		// too generic
		
		// we are allowed to call all the methods of left side type
		// object class methods
		// overridden methods of object class in arraylist class
		
	
	}
}
