package com.design.arraylist.mine;

public class MyArrayList<E> {

	private static final int INITIAL_SIZE = 10;
	private static final float GROWTH_FACTOR = 1.5f;

	private Object[] array;
	private int size;
	private int maxCapacity;

	public MyArrayList() {
		this.size = 0;
		this.maxCapacity = INITIAL_SIZE;
		this.array = new Object[this.maxCapacity];
//		for (int i=0; i<this.maxCapacity; i++) {
//			this.array[i] = null;
//		}
	}

	// TC = O(1) amortized
	// add element to last of array
	public E add(E element) {
		if (this.size == this.maxCapacity) {
			resize();
		}
		this.array[this.size] = element;
		this.size++;
		return element;
	}

	// TC = O(N)
	// add element at index
	public E add(int index, E element) {
		if (index < 0 || index > this.size)  {
			throw new ArrayIndexOutOfBoundsException("Please enter a valid index!");
		}

		if (this.size == this.maxCapacity) {
			resize();
		}

		for (int i=this.size-1; i>=index; i--) {
			this.array[i+1] = this.array[i];
		}
		this.array[index] = element;
		this.size++;
		return (E) this.array[index];

	}

	// TC = O(1)
	// get element from index
	public E get(int index) {
		if (index < 0 || index >= this.size)  {
			throw new ArrayIndexOutOfBoundsException("Please enter a valid index!");
		}
		return (E) this.array[index];
	}

	// TC = O(1)
	public E set(int index, E element) {
		if (index < 0 || index >= this.size)  {
			throw new ArrayIndexOutOfBoundsException("Please enter a valid index!");
		}
		E oldValue = (E) this.array[index];
		this.array[index] = element;
		return oldValue;
	}

	public E remove(int index) {
		if (index < 0 || index >= this.size)  {
			throw new ArrayIndexOutOfBoundsException("Please enter a valid index!");
		}
		E removed = (E) this.array[index];
		for (int i=index; i<this.size-1; i++) {
			this.array[i] = this.array[i+1];
		}
		this.array[this.size-1] = null;
		this.size--;
		return removed;
	}

	public int size() {
		return this.size;
	}

	// helper functions
	private void resize() {
		int newCapacity = (int)(GROWTH_FACTOR * this.maxCapacity);
		Object[] newArray = new Object[newCapacity];
		for (int i=0; i<this.maxCapacity; i++) {
			newArray[i] = this.array[i];
		}
		this.array = newArray;
		this.maxCapacity = newCapacity;
	}

}
