package com.learn.Builder.v1;

public class Builder {
	int studentId;
	String name;
	int rollNo;
	double psp;
	int age;
	String universityName;
	int gradYear;

	public Builder() {
		super();
	}

	public int getStudentId() {
		return studentId;
	}

	public void setStudentId(int studentId) {
		this.studentId = studentId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getRollNo() {
		return rollNo;
	}

	public void setRollNo(int rollNo) {
		this.rollNo = rollNo;
	}

	public double getPsp() {
		return psp;
	}

	public void setPsp(double psp) {
		this.psp = psp;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public String getUniversityName() {
		return universityName;
	}

	public void setUniversityName(String universityName) {
		this.universityName = universityName;
	}

	public int getGradYear() {
		return gradYear;
	}

	public void setGradYear(int gradYear) {
		this.gradYear = gradYear;
	}


	// build the Student class object in builder only
	// do the validation here only!
	// we have removed the validations even out of the constructor also
	// and put it into build() of Builder
	// so we are not calling the Student constructor until we are done with validations

	public Student build() {
		// do all the validations here.
		// validation starts ....
        if(age > 25) {
            throw new IllegalArgumentException();
        }
		// validation ends .....

		// now, build() will build the Student object
		// means, Builder class build() method is creating the Student object
		// Student constructor needs a builder object
		// that has the data values
		// so we pass 'this'
		return new Student(this);
	}
}
