package com.learn.L4_Factory.v1;

import com.learn.L4_Factory.Notification;

import java.util.Scanner;

public class Client {

	public static void main(String[] args) {
		Scanner scn = new Scanner(System.in);
		String input = scn.next();
		scn.close();

		String message = "Hello Kitty!";
		notify(input, message);
	}

	private static void notify(String notificationType, String message) {
		Notification notification = SimpleNotificationFactory.createNotification(notificationType);
		notification.sendNotification(message);
	}
}
