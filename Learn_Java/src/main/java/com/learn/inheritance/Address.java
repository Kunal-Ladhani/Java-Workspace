package com.learn.inheritance;

public class Address implements Cloneable {
	private String address;
	
	public Address() {
		super();
	}
	
	public Address(String address) {
		this.address = address;
	}
 
	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}
	
	@Override
	public Object clone() {
		try {
			return (Address) super.clone();
		} catch(CloneNotSupportedException e) {
			System.out.println("Exception = "+e);
			return new Address(this.address);
		}
	}
}
