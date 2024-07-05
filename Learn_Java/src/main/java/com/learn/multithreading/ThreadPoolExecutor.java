package com.learn.multithreading;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPoolExecutor {
	public static void main(String[] args) {
		PrintJob[] jobs = {
				new PrintJob("Lecture"),
				new PrintJob("Syllabus"),
				new PrintJob("Coding"),
				new PrintJob("MAC"),
				new PrintJob("Admission")
		};
		ExecutorService executorService = Executors.newFixedThreadPool(3);
		// will give us a pool of 3 threads
		// which can be stored inside object of Executor Service
		// which will work on these 5 jobs -> iterate over this array


		for (PrintJob job : jobs) {
			executorService.submit(job);
		}

		// Which thread does which job -> depends on the Executor Service
		// we cannot decide it will handle that

		executorService.shutdown();
		// will shutDown the executor Service


	}
}

class PrintJob implements Runnable {

	String name;

	public PrintJob(String name) {
		this.name = name;
	}

	@Override
	public void run() {
		System.out.println(name + " job is STARTED by the thread => " + Thread.currentThread().getName());
		try {
			Thread.sleep(4000);
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
		}
		System.out.println(name + " job is COMPLETED by the thread => " + Thread.currentThread().getName());
	}

}