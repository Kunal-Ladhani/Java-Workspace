package com.learn.L4_Factory;

public class PushNotification implements Notification {

	@Override
	public void sendNotification(String message) {
		System.out.println("Sent Push Notification " + message);
	}
}
