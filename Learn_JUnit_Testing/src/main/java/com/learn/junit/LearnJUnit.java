package com.learn.junit;

import java.util.Scanner;

/* JUnit - testing framework
 *
 * business logic - the dev/coder can know that the API is doing the required tasks.
 * say I leave the company, then how will a new dev know that this method is doing the
 * expected things.
 *
 * say I came back at this method after years, how would I know what was the business-logic
 * expectation from this method at that time.
 *
 * Now, how do I change the behavior of this method to meet the correct req.
 *
 * 2nd -> how can the machine know if the method is correctly functioning or not.
 *
 * AUTOMATICALLY.
 *
 * means you dont need to all all the APIs again and again for testing manually.
 * machine will do it automatically.
 * the framework also let you set the benchmark for testing the code.
 *
 * These testing frameworks give us this provision to do these things.
 * This is the code implementation...
 * this is the expected behaviour...
 * these are the test cases for it...
 * this test case fails for the code...
 *
 * Junit + mockito
 *
 * Junit libraries -
 *
 * Junit platform
 * Junit jupiter
 * Junit vintage - optional
 *
 * we will use junit 5 it is compatible with java 8 and later.
 * it supports all new features like lambda etc.
 *
 * lets say you have test cases written in older version of junit.
 * for backward compatibility you have junit vintage.
 *
 *
 * for src/test/java
 * you can write any package name, but the convention is ->
 * it should be same as the name of package in which you wrote the business logic.
 *
 */
public class LearnJUnit {
	public static void main(String[] args) {
		Scanner scn = new Scanner(System.in);
		System.out.println("Enter two values - ");
		Integer v1 = scn.nextInt();
		Integer v2 = scn.nextInt();
		scn.close();

		Calculator cal = new Calculator();
		System.out.println(v1 + " + " + v2 + " = " + cal.add(v1, v2));
		System.out.println(v1 + " x " + v2 + " = " + cal.multiply(v1, v2));
	}
}
