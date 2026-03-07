package com.learn.multithreading.thread_synchronization;

public class Driver {

	public static void main(String[] args) throws InterruptedException {

		PersonalThread thread = new PersonalThread();

		Thread.currentThread().setName("Main Thread");

		thread.start();
		//Thread.sleep(5000);

//		try {
//			Thread.sleep(3000);
//		} catch(InterruptedException e) {
//			e.printStackTrace();
//		}

		// we can either put it to sleep but it is very vague,
		// because we don't know how long the thread will take to complete its task.
		// better to use wait method

		synchronized (thread) {
			System.out.println(Thread.currentThread().getName() + " is going into wait state.");
			// this line means that
			// main will wait for personal thread
			thread.wait();
			System.out.println(Thread.currentThread().getName() + " got notified.");
		}

		System.out.println("Total : " + thread.total);
	}

}
