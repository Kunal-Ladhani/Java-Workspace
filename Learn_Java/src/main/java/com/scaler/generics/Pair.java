package com.scaler.generics;

public class Pair<F, S> {
	private F first;
	private S second;

	// no args constructor
	public Pair() {
		this.first = null;
		this.second = null;
	}

	// all args constructor
	public Pair(F first, S second) {
		this.first = first;
		this.second = second;
	}

	public F getFirst() {
		return first;
	}

	public void setFirst(F first) {
		this.first = first;
	}

	public S getSecond() {
		return second;
	}

	public void setSecond(S second) {
		this.second = second;
	}

	public String toString() {
		return "First = " + this.first + " Second = " + this.second;
	}

}
