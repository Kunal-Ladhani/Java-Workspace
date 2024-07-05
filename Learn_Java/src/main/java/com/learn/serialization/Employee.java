package com.learn.serialization;

import java.io.Serializable;

public class Employee implements Serializable {

	//	Serial Version ID
	private static final long serialVersionUID = 1L;

	private transient int empId;
    // transient means that state of this variable will not be stored in .ser file.
    // so no one will be able to read it on the other side of the network.
    private String empName;

    public Employee() {
        super();
    }

    public Employee(int empId, String empName) {
        super();
        this.empId = empId;
        this.empName = empName;
    }

    @Override
    public String toString() {
        return "Employee{" +
                "empId=" + empId +
                ", empName='" + empName + '\'' +
                '}';
    }

    public int getEmpId() {
        return empId;
    }

    public void setEmpId(int empId) {
        this.empId = empId;
    }

    public String getEmpName() {
        return empName;
    }

    public void setEmpName(String empName) {
        this.empName = empName;
    }
}