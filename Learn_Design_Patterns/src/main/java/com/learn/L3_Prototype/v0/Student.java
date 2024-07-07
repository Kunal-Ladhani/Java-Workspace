package com.learn.L3_Prototype.v0;

public class Student implements Prototype<Student> {
    private int studentId;
    private String name;
    private int rollNo;
    private double psp;
    private int age;
    private String universityName;
    private int gradYear;

    public Student() {
        super();
    }

    // copy constructor
    public Student(Student s) {
        this.name = s.name;
        this.age = s.age;
        this.psp = s.psp;
        this.rollNo = s.rollNo;
        this.studentId = s.studentId;
        this.gradYear = s.gradYear;
        this.universityName = s.universityName;
    }

    @Override
    public Student clone() {
//        Student newStudent = new Student();
//        newStudent.setStudentId(this.studentId);
//        newStudent.setName(this.name);
//        newStudent.setAge(this.age);
//        newStudent.setRollNo(this.rollNo);
//        newStudent.setUniversityName(this.universityName);
//        newStudent.setGradYear(this.gradYear);
//        newStudent.setPsp(this.psp);
//        return newStudent;
        return new Student(this);
    }

//    public int getStudentId() {
//        return studentId;
//    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

//    public String getName() {
//        return name;
//    }

    public void setName(String name) {
        this.name = name;
    }

//    public int getRollNo() {
//        return rollNo;
//    }

    public void setRollNo(int rollNo) {
        this.rollNo = rollNo;
    }

//    public double getPsp() {
//        return psp;
//    }

    public void setPsp(double psp) {
        this.psp = psp;
    }

//    public int getAge() {
//        return age;
//    }

    public void setAge(int age) {
        this.age = age;
    }

//    public String getUniversityName() {
//        return universityName;
//    }

    public void setUniversityName(String universityName) {
        this.universityName = universityName;
    }

//    public int getGradYear() {
//        return gradYear;
//    }

    public void setGradYear(int gradYear) {
        this.gradYear = gradYear;
    }

    @Override
    public String toString() {
        return "Student{" +
                "studentId=" + studentId +
                ", name='" + name + '\'' +
                ", rollNo=" + rollNo +
                ", psp=" + psp +
                ", age=" + age +
                ", universityName='" + universityName + '\'' +
                ", gradYear=" + gradYear +
                '}';
    }
}