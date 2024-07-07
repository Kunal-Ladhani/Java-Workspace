package com.learn.L2_Builder.v1;

// this is v1 of the builder design pattern
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
//        if(builder.getAge() > 25) {
//            throw new IllegalArgumentException();
//        }
        // validation ends .....
        this.studentId = builder.getStudentId();
        this.name = builder.getName();
        this.rollNo = builder.getRollNo();
        this.psp = builder.getPsp();
        this.age = builder.getAge();
        this.gradYear = builder.getGradYear();
        this.universityName = builder.getUniversityName();
    }

    // this breaks Single Resp Principle
    // getter method for builder class object
    // static is used because we don't want to associate it to an object,
    // we want to associate it to Student class, so we can directly create builder with class name only.
    public static Builder getBuilder() {
        return new Builder();
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
