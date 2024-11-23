package com.learn.collection;

public class Car implements Comparable<Car> {
	int Price;
	int Speed;

	public Car(int Price, int Speed) {
		this.Price = Price;
		this.Speed = Speed;
	}

	@Override
	public int compareTo(Car c) {
		return Integer.compare(this.Price, c.Price);
	}

	@Override
	public String toString() {
		return String.format("Price = %d, Speed = %d", this.Price, this.Speed);
	}
}
