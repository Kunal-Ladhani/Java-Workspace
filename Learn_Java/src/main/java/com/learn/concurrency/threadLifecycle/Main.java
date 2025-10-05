package com.learn.concurrency.threadLifecycle;

public class Main {

	public static void main(String[] args) throws InterruptedException {
		MyClass myClass = new MyClass();
		Thread t1 = new Thread(myClass);

		System.out.println("T1 in state => " + t1.getState());
		t1.start();

		System.out.println("T1 in state => " + t1.getState());

		Thread.sleep(1000);

		System.out.println("T1 in state => " + t1.getState());
	}

	static class MyClass implements Runnable {

		@Override
		public void run() {

			try {
				Thread current = Thread.currentThread();
				current.interrupt();
//				current.wait(1000);
				System.out.println("Hello Kitty! from " + current.getName());
			} catch (Exception e) {
				System.out.println("Exception : " + e.getMessage());
			}

		}

	}

}
