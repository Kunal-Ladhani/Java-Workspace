package com.learn.L4_Factory;

public class EmailNotification implements Notification {

	@Override
	public void sendNotification(String message) {
		System.out.println("Sent Email Notification " + message);
	}

}
