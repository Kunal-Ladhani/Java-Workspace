package com.learn.L3_Prototype.v0;

public class IntelligentStudent extends Student {
	int iq;

	public IntelligentStudent() {
		super();
	}
	public IntelligentStudent(IntelligentStudent is) {
		super(is);
		this.iq = is.iq;
	}

	@Override
	public IntelligentStudent clone() {
//		IntelligentStudent intelligentStudent = new IntelligentStudent();
//		intelligentStudent.iq = this.iq;
//		intelligentStudent.setStudentId(this.getStudentId());
//		intelligentStudent.setAge(this.getAge());
//		intelligentStudent.setName(this.getName());
//		intelligentStudent.setPsp(this.getPsp());
//		intelligentStudent.setGradYear(this.getGradYear());
//		intelligentStudent.setRollNo(this.getRollNo());
//		intelligentStudent.setUniversityName(this.getUniversityName());
//		return intelligentStudent;
		return new IntelligentStudent(this);

		// resolved the OCP problem
	}
}
