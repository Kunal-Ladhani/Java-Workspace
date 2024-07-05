package com.learn.enums;

public class Driver {
	public static void main(String[] args) {
		Month[] months = Month.values();
		
		for(Month month : months) {
			System.out.println(month.ordinal()+" ==> "+month);
		}
	}
}
