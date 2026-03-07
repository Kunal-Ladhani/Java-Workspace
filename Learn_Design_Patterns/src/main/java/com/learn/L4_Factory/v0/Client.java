package com.learn.L4_Factory.v0;

import com.learn.L4_Factory.*;

import java.util.Scanner;

public class Client {

	/*

	suppose we want ot send notifications to users -
	SMS
	EMAIL
	PUSH_NOTIFICATION
	OTP
	...

	 the list can go on, so how do we select ?

	*/
	public static void main(String[] args) {

		Scanner scn = new Scanner(System.in);
		String input = scn.next();
		scn.close();

		Notification notification;

		if ("SMS".equalsIgnoreCase(input)) {
			notification = new SmsNotification();
		} else if ("OTP".equalsIgnoreCase(input)) {
			notification = new OtpNotification();
		} else if ("EMAIL".equalsIgnoreCase(input)) {
			notification = new EmailNotification();
		} else if ("PUSH_NOTIFICATION".equalsIgnoreCase(input)) {
			notification = new PushNotification();
		} else {
			throw new IllegalArgumentException("Invalid Input");
		}

		notification.sendNotification("Hello Kitty!");

		/*

		Problem -
		1. 	if you want to add one more type of notification you will have to make edit in client side,
			who is trying to send notification and using your notification service.

			OCP is violated!

		 */

	}

}
