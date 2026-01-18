package com.design.arraylist.mine;

public class Driver {
	public static void main(String[] args) {
		MyArrayList<String> names = new MyArrayList<>();
		names.add("kunal");
		names.add("rohit");
		names.add("kaushik");
		names.add("kamal");
		System.out.println("length = " + names.size());

		System.out.println("removed element at 0 = " + names.remove(0));
		System.out.println("element at 0 = " + names.get(0));
		System.out.println("removed element at 0 = " + names.remove(0));
		System.out.println("element at 0 = " + names.get(0));
		System.out.println("element at 23 = " + names.get(23));
	}
}
