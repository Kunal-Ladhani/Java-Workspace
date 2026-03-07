package com.learn.L4_Factory.v0;

public class EmailNotification implements Notification {

	@Override
	public void sendNotification() {
		System.out.println("Sent Email!");
	}

}
