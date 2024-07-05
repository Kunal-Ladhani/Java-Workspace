package com.learn.dateTimeAPI;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class Driver {
	public static void main(String[] args) {
		LocalDate ld = LocalDate.now();
		System.out.println(ld);

		LocalDateTime lt = LocalDateTime.now();
		System.out.println(lt.getHour() + " : " + lt.getMinute() + " : " + lt.getSecond());

		LocalDateTime dob = LocalDateTime.of(1998, 02, 28, 00, 00, 00);
		System.out.println(dob);

		DateTimeFormatter formatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
		System.out.println(lt.format(formatObj));

		System.out.println(ChronoUnit.YEARS.between(dob, lt));
	}
}
