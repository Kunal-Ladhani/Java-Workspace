package com.learn.basics;

public class PassByValue {
	public static void add(Integer a, Integer b) {
		a = 10;
		System.out.println("Function : "+(a+b));
	}
	
	public static void main(String[] args) {
		Integer a = 2;
		Integer b = 3;
		add(a,b);
		System.out.println("main : "+(a+b));
		
	}
}
