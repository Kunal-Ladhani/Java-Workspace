package com.learn.Builder.v0;

// this is v0 of the builder design pattern
public class Student {
    int studentId;
    String name;
    int rollNo;
    double psp;
    int age;
    String universityName;
    int gradYear;

    /*
    old way was ki you have a lot of constructors... telescopic constructor
    public Student(int studentId, String name ... ) {
        ----
    }
     */

    public Student(Builder builder) {
        // validation starts ....
        if(builder.getAge() > 25) {
            throw new IllegalArgumentException();
        }
        // validation ends .....
        this.studentId = builder.getStudentId();
        this.name = builder.getName();
        this.rollNo = builder.getRollNo();
        this.psp = builder.getPsp();
        this.age = builder.getAge();
        this.gradYear = builder.getGradYear();
        this.universityName = builder.getUniversityName();
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
