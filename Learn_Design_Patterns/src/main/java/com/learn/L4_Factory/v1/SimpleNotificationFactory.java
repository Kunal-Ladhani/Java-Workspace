package com.learn.L4_Factory.v1;

import com.learn.L4_Factory.*;

public class SimpleNotificationFactory {

	public static Notification createNotification(String type) {
		return switch (type) {
			case "SMS" -> new SmsNotification();
			case "OTP" -> new OtpNotification();
			case "EMAIL" -> new EmailNotification();
			case "PUSH_NOTIFICATION" -> new PushNotification();
			default -> throw new IllegalArgumentException("Invalid type");
		};
	}

}
