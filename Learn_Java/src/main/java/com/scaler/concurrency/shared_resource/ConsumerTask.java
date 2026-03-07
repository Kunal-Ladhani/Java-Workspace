package com.scaler.concurrency.shared_resource;

public class ConsumerTask implements Runnable {

	private SharedResource sharedResource;

	public ConsumerTask(SharedResource sharedResource) {
		this.sharedResource = sharedResource;
	}

	@Override
	public void run() {
		System.out.println("Inside run() of Consumer Task : " + Thread.currentThread().getName());
		sharedResource.removeItem();
	}
}
