package com.learn.multithreading.thread_pool_executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Driver {

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
		// will shut down the executor Service
	}

}
