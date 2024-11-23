package com.learn.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class LearnStack {
	public static void main(String[] args) {
//		Stack<Integer> stack = new Stack<>();
//		Deque<Integer> stack1 = new ArrayDeque<>();
//		Deque<Integer> stack2 = new LinkedList<>();
//
//		stack.push(1);
//		stack.pop();
//		stack.peek();
//		stack.empty();
//		stack.size();
//
//		stack1.push(2);
//		stack1.pop();
//		stack1.peek();
//		stack1.isEmpty();
//		stack1.size();
//
//		stack2.push(3);
//		stack2.pop();
//		stack2.peek();
//		stack2.isEmpty();
//		stack2.size();
//
		Car[] arr = {
				new Car(1, 2),
				new Car(2, 3),
				new Car(-1, 5)
		};
		Arrays.sort(arr);
		System.out.println(Arrays.toString(arr));

		ArrayList<Car> cars = new ArrayList<>();
		cars.add(new Car(10, 2));
		cars.add(new Car(5, 1));
		cars.add(new Car(1, 5));

		Collections.sort(cars);
		System.out.println(cars);

		cars.forEach((Car c) -> {
			System.out.println(c);
		});
	}
}
