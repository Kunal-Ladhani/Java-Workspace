package com.learn.concurrency.sharedResource;

public class SharedResource {

	private boolean itemAvailable;

	public SharedResource() {
		this.itemAvailable = false;
	}

	// synchronized will put the monitor lock
	public synchronized void addItem() {
		this.itemAvailable = true;
		System.out.println("Item added by " + Thread.currentThread().getName());
		notifyAll();
	}


	public synchronized void removeItem() {
		System.out.println("removeItem invoked by " + Thread.currentThread().getName());

		// using while to avoid 'spurious wake up' sometime because of system noise
		while (!this.itemAvailable) {
			try {
				System.out.println("Thread " + Thread.currentThread().getName() + " is waiting now!");
				wait();    // releases the monitor lock
			} catch (InterruptedException e) {
				System.out.println("Thread " + Thread.currentThread().getName() + " interrupted!");
			}
		}

		System.out.println("Item removed by " + Thread.currentThread().getName());
		this.itemAvailable = false;
	}

}
