package com.learn.concurrency.producerConsumerProblem;

public class SharedBuffer<T> {

	// fixed size queue
	private Object[] queue;
	private int capacity;
	private int size;
	private int front;
	private int rear;

	public SharedBuffer(int capacity) {
		this.capacity = capacity;
		queue = new Object[this.capacity];
		this.size = 0;
		this.front = 0;
		this.rear = 0;
	}

	public synchronized void producer(T item) {
		// wait while queue is full
		while (capacity == size) {
			try {
				System.out.println("Thread: " + Thread.currentThread().getName() + " is waiting!");
				wait();
			} catch (InterruptedException e) {
				System.out.println("Thread: " + Thread.currentThread().getName() + " interrupted while waiting!");
			}
		}

		// enqueue logic
		queue[rear] = item;
		rear = (rear + 1) % capacity;
		size++;

		System.out.println("Thread: " + Thread.currentThread().getName() + " has produced: " + item);
		notify();    // notify consumer that there are item available in buffer to be consumed.
	}

	public synchronized T consume() {
		// wait while queue is empty
		while (size == 0) {
			try {
				System.out.println("Thread: " + Thread.currentThread().getName() + " is waiting!");
				wait();
			} catch (InterruptedException e) {
				System.out.println("Thread: " + Thread.currentThread().getName() + " interrupted while waiting!");
			}
		}

		// dequeue logic
		T removedItem = (T) queue[front];
		queue[front] = null;
		front = (front + 1) % capacity;
		size--;

		System.out.println("Thread: " + Thread.currentThread().getName() + " has consumed:" + removedItem);
		notify();    // notify the producer that there is space in buffer.

		return removedItem;
	}


}
