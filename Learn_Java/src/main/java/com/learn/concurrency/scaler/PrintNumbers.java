package com.learn.concurrency.scaler;

public class Printer implements Runnable {

	private int num;

	public Printer(int num) {
		this.num = num;
	}

	@Override
	public void run() {
		System.out.println(Thread.currentThread().getName() + " printed : " + this.num);
	}

}
