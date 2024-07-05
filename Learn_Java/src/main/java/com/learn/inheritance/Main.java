package com.learn.inheritance;

public class Main {
	
	public int fun1() {
		byte x = 10;
		return x;
	}
	
	public Object fun2(int x) {
		if(x>10) {
			return new SonyOldTV();
		} else {
			return new SonySmartTV();
		}
	}
	

	
	public static void main(String[] args) {
		
		SonyOldTV remote = new SonySmartTV();
		
		// always check for null if method is returning an object
		if(remote != null) {
			//remote.hello();
		}
		
		// either create a new object or use object downcasting
		SonySmartTV smartRemote = (SonySmartTV)remote;
		// object downcasting
		
		Main m = new Main();
		
		// byte b = m.fun1(); -> this will give error.
		int i = m.fun1();
		long l = m.fun1();
	
		Object obj = new SonySmartTV();
		/*
		 * Look at left - all methods of object (left side)class can be called - parent class
		 * if it has overridden some method - that can be called
		 * specific methods of child class cannot be called
		 * for that you need to do object downcasting
		 */
		
		Object rem = m.fun2(2);
		
		if(rem instanceof SonyOldTV) {
			SonyOldTV rem1 = (SonyOldTV)rem;
			rem1.hello();			
		}
		if(rem instanceof SonySmartTV){
			SonySmartTV rem1 = (SonySmartTV)rem;
			rem1.hello();
			rem1.playGame();
		}		
	}
}
