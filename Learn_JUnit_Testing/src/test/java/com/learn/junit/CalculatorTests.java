package com.learn.junit;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class CalculatorTests {

	// we will write a special method here which will test the method written in src/main/java
	// annotate the method with @Test
	// it will become a test case.
	// these methods will be invoked by the junit

	// these test cases are executed at time of building the application.
	// jenkins run test suites, which are automatically detected by these build tools
	// they will run all the test cases first.
	// if any of these cases fail then it will not deploy the application on server.
	// always write all variations of test cases in the API you built.
	// otw not accepted.

	Calculator calc = null;

	// @BeforeAll and @AfterAll need to be static methods!
	@BeforeAll
	public static void beforeAllTestCases() {
		System.out.println("will run once before all test cases.");
	}

	@AfterAll
	public static void afterAllTestCases() {
		System.out.println("will run once after all test cases.");
	}


	@Test
	@Disabled
	public void testNotRequiredAnymore() {
		assertTrue(false);
		// say you have a few test cases written in this method
		// they are not req now
		// don't delete just annotate with @Disabled - will be skipped
		// @Disabled has also got many versions - try and explore!
	}

	// you can give any name to ALL these methods.
	@BeforeEach
	public void beforeEachTestCaseExecution() {
		System.out.println("Testing begins...");
		calc = new Calculator();
		// write logic you want to run before EACH test case begins.
	}

	@AfterEach
	public void afterEachTestCaseExecution() {
		System.out.println("Testing ends...");
		// write logic you want to run after EACH test case ends.
	}

	@Test
	@DisplayName(value = "Test for add() method.")
	public void testAdd() {
		// we write the assert statements here
		// it is available for all types.
		
	/*
		String msg = "Hello World";
		assertEquals("hello world", msg);
	*/

		// 1st arg = expected output
		// 2nd arg = actual output
		// if they are equal then test case is passed

		assertEquals(24, calc.add(12, 12));
	}

	@Test
	@DisplayName(value = "Just some dummy test cases.")
	public void dummyTests() {
		String actualVal = "actual value";
		// test passed if expected value is equal to actual value.
//		assertEquals("expected value", actualVal);

		// test passed if boolean exp is true.
//		assertTrue(5 == 6);

		// test passed if boolean exp is false.
		assertFalse(5 == 6);

		assertThrows(ArithmeticException.class, () -> {
			int res = 5 / 0;
		});

		assertNull(null);

		assertNotNull(calc);
	}

	// when you might get a different result in nth run
	// example is multithreaded application.
	@RepeatedTest(name = "Repeated Test", value = 5)
	public void shouldThrowException() {
		Exception e = assertThrows(ClassCastException.class, () -> calc.multiply(12, "12"));
		assertEquals("Enter valid integer values.", e.getMessage());
	}

	// to test REST endpoints, methods connected to external layers => DAO to DB conn.
	// testing REST APIs, testing 3rd party apis => mocking.
	// use Mockito framework.

	@Test
	public void testingBooleanFunction() {
		assertTrue(calc.methodReturnsBooleanValue());
	}

	@Test
	public void testAssertNull() {
		String s1 = "abc";
		String s2 = null;
		String s3 = new String();

		assertNotNull(s1);
		assertNull(s2);
		assertEquals("", s3);
	}

	@Test
	public void assertAll() {
		String s1 = "abc";
		String s2 = "mno";
		String s3 = "xyz";

		// here you can test all the assertions in one test case.
		Assertions.assertAll("nums", () -> assertEquals("abc", s1), () -> assertEquals("mno", s2), () -> assertEquals("xyz", s3));
	}

}
