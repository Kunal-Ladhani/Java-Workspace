package com.learn.inheritance;

public class SonySmartTV extends SonyOldTV {
	@Override
	public void hello() {
		System.out.println("Hello from smart TV");
	}
	
	public void playGame() {
		System.out.println("playing games now");
	}
}
