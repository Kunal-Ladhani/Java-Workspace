package com.learn.L4_Factory;

public class SmsNotification implements Notification {

	@Override
	public void sendNotification(String message) {
		System.out.println("Sent SMS Notification " + message);
	}

}
