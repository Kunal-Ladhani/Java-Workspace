package com.learn.serialization;

public class PartTimeEmployee extends Employee  {

	private static final long serialVersionUID = 1L;
	private int ratePerHour;
	
	public PartTimeEmployee() {
		super();
	}

	public PartTimeEmployee(int empId, String empName, int ratePerHour) {
		super(empId, empName);
		this.ratePerHour = ratePerHour;
	}
	
}
