package com.design.hashmap;

import com.design.hashmap.mine.MyHashMap;

public class Driver {

	public static void main(String[] args) {
		MyHashMap<String, Integer> map = new MyHashMap<>();
		map.put("kunal", 6);
		map.put("rohit", 7);
		map.put("kaushik", 5);

		System.out.println("rohit = " + map.get("rohit"));
		System.out.println("kunal = " + map.get("kunal"));
		System.out.println("kaushik = " + map.get("kaushik"));

//		map.remove("kaushik");
		map.put("kaushik", 55);
		System.out.println("kaushik = " + map.get("kaushik"));


	}

}
