package com.learn.collection;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

public class LearnMap {
	public static void main(String[] args) {
		
		System.out.println("-----------------------HASH MAP----------------------------");
		Map<Integer, String> map = new HashMap<>();
		// Map<Integer, String> map = new LinkedHashMap<>();

		// LinkedHashMap will preserve the insertion order
		// In HashMap the order will be according to the hashCode that is generated
		map.put(2, "Kanak");
		map.put(1, "Kunal");
		map.put(4, "Rohit");
		map.put(3, "Vaibhav");
		
		System.out.println(map);
		//System.out.println(map.get(4));
		// get() returns null if not been added
		//map.remove(2);
		
		// Iterate over the map
		Set<Integer> keySet = map.keySet();
		keySet.forEach(key -> System.out.println(key));
		
		for(Integer key : keySet) {
			System.out.println(key + "=>" + map.get(key));
		}
		
		Collection<String> col = map.values();
		for(String val : col) {
			System.out.println(val);
		}
		
		// using lambda function
		Set<Map.Entry<Integer, String>> entrySet = map.entrySet();
		entrySet.forEach(entry -> System.out.println(entry.getKey() + "++>" + entry.getValue()));
		
		// for each loop
		for(Map.Entry<Integer, String> entry : entrySet) {
			System.out.println(entry.getKey() +" -> "+entry.getValue());
		}
		
		System.out.println("---------------------TREE MAP --------------------------");
		
		Map<Student,Integer> treeMap = new TreeMap<>(new StudentRollComp());
		// tree map is sorted map
		// you need to pass a comparator so that it can sort the tree
		// Wrapper classes already have comparator
		
		treeMap.put(new Student(49, "Kunal"), 61);
		treeMap.put(new Student(42,"Bhushan"),62);
		treeMap.put(new Student(56,"Rohit"),63);
		treeMap.put(new Student(11,"Tarun"),64);
		treeMap.put(new Student(1,"Kaushal"),65);
		treeMap.put(new Student(65,"Ajay"),66);
		
		//System.out.println(treeMap);
		Set<Map.Entry<Student, Integer>> students = treeMap.entrySet();
		for(Entry<Student, Integer> student : students) {
			System.out.println("Roll No. =>"+student.getKey().getRoll());
			System.out.println("Name =>"+student.getKey().getName());
			System.out.println("Marks =>"+student.getValue());
			System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++");
		}
	}
}
