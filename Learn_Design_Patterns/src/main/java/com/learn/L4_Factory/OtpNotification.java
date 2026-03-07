package com.learn.L4_Factory;

public class OtpNotification implements Notification {

	@Override
	public void sendNotification(String message) {
		System.out.println("Sent OTP Notification " + message);
	}
}
