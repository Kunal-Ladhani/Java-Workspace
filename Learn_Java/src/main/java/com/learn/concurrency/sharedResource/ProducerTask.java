package com.learn.concurrency.sharedResource;

public class ProducerTask implements Runnable {

	private SharedResource sharedResource;

	ProducerTask(SharedResource sharedResource) {
		this.sharedResource = sharedResource;
	}

	@Override
	public void run() {
		System.out.println("Inside run() of Producer Task : " + Thread.currentThread().getName());
		try {
			Thread.sleep(5000l);
		} catch (InterruptedException e) {
			System.out.println("Producer Task run() sleep was interrupted! " + e.getMessage());
		}
		sharedResource.addItem();
	}

}
