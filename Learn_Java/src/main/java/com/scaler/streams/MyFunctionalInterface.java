package com.scaler.streams;


@FunctionalInterface
public interface MyFunctionalInterface {

	// Functional Interface = interface with SAM - single abstract method
	// only one method that other impl class needs to implement
	// remaining are default or static methods !
	// allows for functional programming
	void myMethod(Integer type);

}
